package dev.aeternum;
import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

final class SpicyLyricsClient {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private long blockedUntil;
    private static LyricsController.Result message(String text,boolean retry){return new LyricsController.Result(List.of(),List.of(),text,retry);}
    LyricsController.Result lookup(String id,String key){
        if(!id.matches("[A-Za-z0-9]{22}"))return message("Spicy Lyrics: invalid track ID",false);
        if(!key.startsWith("sl_pk_"))return message("Spicy Lyrics: use a client key (sl_pk_)",false);
        if(System.currentTimeMillis()<blockedUntil)return message("Spicy Lyrics: rate limit; using LRCLIB",true);
        try{
            var request=HttpRequest.newBuilder(URI.create("https://api.spicylyrics.org/v1/lyrics/"+id)).header("Authorization","Bearer "+key).header("Accept","application/json").header("User-Agent","Aeternum/0.6.0").timeout(Duration.ofSeconds(7)).GET().build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var in=response.body()){
                int status=response.statusCode();
                if(status==429){long delay=60000;try{delay=Math.max(delay,Long.parseLong(response.headers().firstValue("Retry-After").orElse("60"))*1000);}catch(Exception ignored){}blockedUntil=System.currentTimeMillis()+delay;return message("Spicy Lyrics: rate limit; using LRCLIB",true);}
                if(status==401||status==403)return message("Spicy Lyrics: check key / native-client access",false);
                if(status==404)return message("Spicy Lyrics: no match; using LRCLIB",false);
                if(status!=200)return message("Spicy Lyrics unavailable; using LRCLIB",true);
                byte[] body=in.readNBytes(1_000_001);if(body.length>1_000_000)return message("Spicy Lyrics response too large",false);
                return parse(JsonParser.parseString(new String(body,StandardCharsets.UTF_8)).getAsJsonObject());
            }
        }catch(Exception e){return message("Spicy Lyrics unavailable; using LRCLIB",true);}
    }
    static LyricsController.Result parse(JsonObject envelope){
        if(!envelope.has("Body")||!envelope.get("Body").isJsonObject())return message("Spicy Lyrics: no lyrics",false);
        var body=envelope.getAsJsonObject("Body");String type=text(body,"Type"),source=text(body,"source");
        List<LyricsController.Credit> credits=new ArrayList<>();
        String provider=switch(source){case "spicy_lyrics"->"Spicy Lyrics";case "apple_music"->"Apple Music";case "spotify"->"Spotify";default->"Unknown source";};
        credits.add(new LyricsController.Credit("Lyrics from "+provider,"https://developers.spicylyrics.org"));
        if(source.equals("spicy_lyrics")&&body.has("UploadAttribution")){
            var attribution=body.getAsJsonObject("UploadAttribution");for(String role:List.of("Uploader","Maker")){
                if(!attribution.has(role)||!attribution.get(role).isJsonObject())continue;var person=attribution.getAsJsonObject(role);String name=text(person,"username"),url=text(person,"url");
                if(!name.isBlank())credits.add(new LyricsController.Credit((role.equals("Uploader")?"Uploaded by ":"Made by ")+name,safeLink(url)));
            }
        }
        List<LyricsController.Line> lines=new ArrayList<>();List<String> plain=new ArrayList<>();
        if(body.has("Content")&&body.get("Content").isJsonArray())for(var entry:body.getAsJsonArray("Content")){
            if(lines.size()+plain.size()>5000)break;
            if(entry.isJsonPrimitive()){plain.add(entry.getAsString());continue;}if(!entry.isJsonObject())continue;
            var row=entry.getAsJsonObject();if(text(row,"Type").equals("Interlude")){lines.add(new LyricsController.Line(number(row,"StartTime",0),""));continue;}
            var lead=row.has("Lead")&&row.get("Lead").isJsonObject()?row.getAsJsonObject("Lead"):row;
            if(lead.has("Syllables")&&lead.get("Syllables").isJsonArray()){
                StringBuilder words=new StringBuilder();List<LyricsController.Word> timings=new ArrayList<>();
                for(var syllable:lead.getAsJsonArray("Syllables")){
                    var syll=syllable.getAsJsonObject();String part=text(syll,"Text");if(words.length()+part.length()>4096)break;int from=words.length();words.append(part);int to=words.length();
                    double start=number(syll,"StartTime",0),end=number(syll,"EndTime",start);timings.add(new LyricsController.Word(start,Math.max(start,end),from,to));
                    if(!syll.has("IsPartOfWord")||!syll.get("IsPartOfWord").getAsBoolean())words.append(' ');
                }
                lines.add(new LyricsController.Line(number(lead,"StartTime",timings.isEmpty()?0:timings.getFirst().start()),words.toString().stripTrailing(),timings));
            }else {String words=text(lead,"Text");if(words.isEmpty())words=text(row,"Text");if(type.equals("Static"))plain.add(words);else lines.add(new LyricsController.Line(number(lead,"StartTime",number(row,"StartTime",0)),words));}
        }
        lines.sort(Comparator.comparingDouble(LyricsController.Line::seconds));
        if(lines.isEmpty()&&plain.isEmpty())return message("Spicy Lyrics: no usable lyrics",false);
        return new LyricsController.Result(lines,plain,lines.isEmpty()?"Unsynced lyrics · open Lyrics settings":"Spicy Lyrics",false,"",credits);
    }
    private static String safeLink(String value){try{var uri=URI.create(value);return "https".equals(uri.getScheme())&&uri.getHost()!=null?uri.toString():"";}catch(Exception e){return "";}}
    private static String text(JsonObject j,String key){return j.has(key)&&j.get(key).isJsonPrimitive()?j.get(key).getAsString():"";}
    private static double number(JsonObject j,String key,double fallback){try{double d=j.get(key).getAsDouble();return Double.isFinite(d)&&d>=0?d:fallback;}catch(Exception e){return fallback;}}
}
