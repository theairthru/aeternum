package dev.aeternum;

import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Track-level LRCLIB lookups, cached and entirely off the render thread. */
public final class LyricsController {
    public record Word(double start,double end,int from,int to){}
    public record Line(double seconds,String text,List<Word> words){public Line(double seconds,String text){this(seconds,text,List.of());}public Line{words=List.copyOf(words);}}
    public record Credit(String label,String url){}
    public record Result(List<Line> timed,List<String> plain,String notice,boolean retryable,String credit,List<Credit> credits){
        public Result(List<Line> timed,List<String> plain,String notice,boolean retryable){this(timed,plain,notice,retryable,"",List.of());}
        public Result(List<Line> timed,List<String> plain,String notice,boolean retryable,String credit){this(timed,plain,notice,retryable,credit,List.of(new Credit("Lyrics powered by Musixmatch","https://www.musixmatch.com")));}
        public Result {timed=List.copyOf(timed);plain=List.copyOf(plain);credits=List.copyOf(credits);}
    }
    private static final Pattern TIME=Pattern.compile("\\[(\\d{1,3}):(\\d{2}(?:\\.\\d{1,3})?)\\]");
    private static final Pattern OFFSET=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE);
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"Aeternum lyrics");t.setDaemon(true);return t;});
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private record Cached(Result result,long at){}
    private final Map<String,Cached> cache=new LinkedHashMap<>();
    public volatile String providerStatus="LRCLIB fallback";
    private volatile String selected="";
    private String candidate="";
    private long candidateSince,retryAt;
    private volatile long blockedUntil;
    private long lastRequest;
    private volatile long publishedAt;
    public LyricsController(){worker.scheduleAtFixedRate(()->{
        long cutoff=System.currentTimeMillis()-86_400_000;cache.entrySet().removeIf(e->e.getValue().at()<cutoff);
        synchronized(this){if(publishedAt>0&&publishedAt<cutoff){selected=candidate="";result=notice("Waiting for a track",false);publishedAt=0;}}
    },1,1,TimeUnit.HOURS);}
    public volatile Result result=notice("Waiting for a track",false);
    private static Result notice(String text,boolean retry){return new Result(List.of(),List.of(),text,retry);}
    public synchronized void refresh(){selected=candidate="";result=notice("Finding lyrics…",false);worker.submit(cache::clear);}
    public synchronized void clear(){if(!selected.isEmpty()||!candidate.isEmpty()){selected=candidate="";result=notice("Waiting for a track",false);}}
    public synchronized void observe(String title,String artist,String album,double duration){observe(title,artist,album,duration,"");}
    public synchronized void observe(String title,String artist,String album,double duration,String spotifyId){
        if(title.isBlank()||artist.isBlank()){clear();return;}
        String cleanTitle=title.replaceAll("(?i)\\s*[\\[(](?:official\\s+)?(?:music\\s+)?(?:video|audio|lyrics?|lyric video|visuali[sz]er)[\\])]","").strip();
        String cleanArtist=artist.replaceAll("(?i)(?:\\s*-\\s*Topic|VEVO)$","").strip();
        String[] parts=cleanTitle.split(" - ",2);
        if(parts.length==2&&normal(parts[0]).equals(normal(cleanArtist)))cleanTitle=parts[1];
        String key=cleanTitle+'\n'+cleanArtist+'\n'+album+'\n'+Math.round(duration)+'\n'+spotifyId;
        long now=System.currentTimeMillis();
        if(!key.equals(candidate)){candidate=key;candidateSince=now;selected="";result=notice("Finding lyrics…",false);return;}
        if(now-candidateSince<750||key.equals(selected)&&now<retryAt)return;
        selected=key;retryAt=Long.MAX_VALUE;
        String track=cleanTitle,performer=cleanArtist;
        worker.submit(()->{
            if(!key.equals(selected))return;
            Cached cached=cache.get(key);Result found=cached!=null&&System.currentTimeMillis()-cached.at()<86_400_000?cached.result():null;
            if(found==null){
                try{found=lookup(track,performer,album,duration,spotifyId);}catch(Exception e){found=notice(System.currentTimeMillis()<blockedUntil?"Lyrics rate-limited; retrying later":"Lyrics unavailable; retrying later",true);Aeternum.LOG.debug("Lyrics lookup unavailable",e);}
                if(!found.retryable()){cache.put(key,new Cached(found,System.currentTimeMillis()));while(cache.size()>64)cache.remove(cache.keySet().iterator().next());}
            }
            synchronized(this){if(key.equals(selected)){result=found;publishedAt=System.currentTimeMillis();retryAt=found.retryable()?Math.max(System.currentTimeMillis()+30000,blockedUntil):System.currentTimeMillis()+86_400_000;}}
        });
    }
    private final SpicyLyricsClient spicy=new SpicyLyricsClient();
    private Result lookup(String title,String artist,String album,double duration,String spotifyId)throws Exception{
        Result primary=null;
        if(Aeternum.config.spicyApiKey.isBlank())providerStatus="Spicy Lyrics: add a client key";
        else if(spotifyId.isBlank())providerStatus="Spicy Lyrics: waiting for Spicetify";
        else {
            primary=spicy.lookup(spotifyId,Aeternum.config.spicyApiKey);
            providerStatus=primary.notice();
            if(!primary.timed().isEmpty())return primary;
        }
        Result fallback;try{fallback=lookupLrclib(title,artist,album,duration);}catch(Exception e){return primary!=null&&!primary.plain().isEmpty()?primary:notice("Lyrics unavailable; retrying later",true);}
        if(!fallback.timed().isEmpty()||!fallback.plain().isEmpty())return fallback;
        return primary!=null&&(!primary.plain().isEmpty()||primary.retryable())?primary:fallback;
    }
    private Result lookupLrclib(String title,String artist,String album,double duration)throws Exception{
        JsonObject record=null;
        if(!album.isBlank()&&duration>0){
            JsonElement exact=request("get?track_name="+encode(title)+"&artist_name="+encode(artist)+"&album_name="+encode(album)+"&duration="+Math.round(duration));
            if(exact!=null&&exact.isJsonObject())record=exact.getAsJsonObject();
        }
        if(record==null){
            JsonElement search=request("search?track_name="+encode(title)+"&artist_name="+encode(artist));
            if(search!=null&&search.isJsonArray())record=choose(search.getAsJsonArray(),title,artist,duration);
        }
        return record==null?notice("No matching lyrics on LRCLIB",false):fromJson(record);
    }
    static JsonObject choose(JsonArray records,String title,String artist,double duration){
        JsonObject best=null;double difference=Double.MAX_VALUE;
        for(var value:records){if(!value.isJsonObject())continue;var r=value.getAsJsonObject();
            if(!normal(string(r,"trackName")).equals(normal(title))||!normal(string(r,"artistName")).equals(normal(artist)))continue;
            double d=r.has("duration")?r.get("duration").getAsDouble():0,delta=Math.abs(d-duration);
            if(duration>0&&delta>3)continue;
            if(best==null||delta<difference){best=r;difference=delta;}
        }
        return best;
    }
    private JsonElement request(String path)throws Exception{
        if(System.currentTimeMillis()<blockedUntil)throw new IllegalStateException("LRCLIB cooldown");
        long delay=350-(System.currentTimeMillis()-lastRequest);if(delay>0)Thread.sleep(delay);lastRequest=System.currentTimeMillis();
        var req=HttpRequest.newBuilder(URI.create("https://lrclib.net/api/"+path)).timeout(Duration.ofSeconds(10)).header("User-Agent","Aeternum/0.6.0 (personal Minecraft client)").header("Accept","application/json").GET().build();
        var response=http.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode()==404)return null;
        if(response.statusCode()==429){
            String after=response.headers().firstValue("Retry-After").orElse("60");long wait=60000;
            try{wait=Math.max(1000,Long.parseLong(after)*1000);}catch(Exception e){try{wait=Math.max(1000,java.time.ZonedDateTime.parse(after,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis());}catch(Exception ignored){}}
            blockedUntil=System.currentTimeMillis()+wait;throw new IllegalStateException("LRCLIB rate limit");
        }
        if(response.statusCode()!=200||response.body().length()>1_000_000)throw new IllegalStateException("Lyrics response unavailable");
        return JsonParser.parseString(response.body());
    }
    static Result fromJson(JsonObject j){
        if(j.has("instrumental")&&j.get("instrumental").getAsBoolean())return notice("Instrumental",false);
        List<Line> timed=parseLrc(string(j,"syncedLyrics"));String plain=string(j,"plainLyrics");
        return new Result(timed,plain.isBlank()?List.of():Arrays.asList(plain.split("\\R")),timed.isEmpty()?(plain.isBlank()?"No lyrics available":"Unsynced lyrics · open Lyrics settings"):"LRCLIB",false);
    }
    static List<Line> parseLrc(String text){
        var offset=OFFSET.matcher(text);double shift=offset.find()?Double.parseDouble(offset.group(1))/1000:0;
        List<Line> result=new ArrayList<>();
        for(String row:text.split("\\R")){var times=TIME.matcher(row);List<Double> stamps=new ArrayList<>();int end=0;
            while(times.find()){stamps.add(Integer.parseInt(times.group(1))*60+Double.parseDouble(times.group(2))-shift);end=times.end();}
            if(end>0){String words=row.substring(end).strip();for(double stamp:stamps)result.add(new Line(stamp,words));}
            if(result.size()>5000)break;
        }
        result.sort(Comparator.comparingDouble(Line::seconds));return List.copyOf(result);
    }
    public int index(double position){List<Line> lines=result.timed();int lo=0,hi=lines.size()-1,best=-1;while(lo<=hi){int mid=(lo+hi)>>>1;if(lines.get(mid).seconds()<=position){best=mid;lo=mid+1;}else hi=mid-1;}return best;}
    private static String normal(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
    private static String string(JsonObject j,String field){return j.has(field)&&!j.get(field).isJsonNull()?j.get(field).getAsString():"";}
    private static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
}
