package dev.aeternum;
import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Authenticated loopback-only metadata bridge; no control endpoints or lyric data. */
public final class SpotifyBridge {
    public record Track(String id,String title,String artist,String album,double duration,long at){}
    private static volatile Track track;
    private static final PlaybackClock clock=new PlaybackClock();
    private static HttpServer server;
    private static ExecutorService executor;
    private static long retry;
    public static volatile String status="Not connected";
    private static final Set<String> ORIGINS=Set.of("https://xpui.app.spotify.com","https://open.spotify.com");
    public static synchronized void ensureStarted(){
        if(!Aeternum.config.spotifyBridgeEnabled){if(server!=null){server.stop(0);server=null;if(executor!=null)executor.shutdownNow();status="Disconnected";}track=null;return;}
        if(server!=null||System.currentTimeMillis()<retry)return;
        try{
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",38467),4);
            server.createContext("/track",SpotifyBridge::handle);
            executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Aeternum Spotify bridge");t.setDaemon(true);return t;});server.setExecutor(executor);
            server.start();status="Waiting for Spicetify";
            Runtime.getRuntime().addShutdownHook(new Thread(()->{if(server!=null)server.stop(0);}));
        }catch(Exception e){server=null;retry=System.currentTimeMillis()+30000;status="Cannot open local port 38467";}
    }
    public static synchronized String pair(){
        if(!Aeternum.config.spotifyPairToken.matches("[a-f0-9]{64}")){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);Aeternum.config.spotifyPairToken=HexFormat.of().formatHex(bytes);}
        Aeternum.config.spotifyBridgeEnabled=true;Aeternum.config.save();ensureStarted();return Aeternum.config.spotifyPairToken;
    }
    public static Track current(){var t=track;return Aeternum.config.spotifyBridgeEnabled&&t!=null&&System.nanoTime()-t.at()<5_000_000_000L?t:null;}
    public static double position(){return clock.position(System.nanoTime());}
    static void accept(JsonObject j){
        String id=string(j,"id");if(id.isEmpty()){track=null;clock.reset();return;}
        if(!id.matches("[A-Za-z0-9]{22}"))throw new IllegalArgumentException();
        double duration=j.get("duration").getAsDouble(),position=j.get("position").getAsDouble();
        if(!Double.isFinite(duration)||!Double.isFinite(position)||duration<=0||duration>86400||position<0||position>duration+2)throw new IllegalArgumentException();
        String title=string(j,"title"),artist=string(j,"artist");if(title.isBlank()||artist.isBlank())throw new IllegalArgumentException();
        long now=System.nanoTime();clock.update(id,position,duration,j.get("playing").getAsBoolean(),now);track=new Track(id,title,artist,string(j,"album"),duration,now);status="Connected to Spicetify";
    }
    private static String string(JsonObject j,String key){String s=j.has(key)?j.get(key).getAsString():"";if(s.length()>1024)throw new IllegalArgumentException();return s;}
    private static void handle(HttpExchange e)throws java.io.IOException{
        try{
            if(!e.getRequestURI().getPath().equals("/track")){reply(e,404);return;}
            String origin=e.getRequestHeaders().getFirst("Origin");
            if(origin!=null&&!ORIGINS.contains(origin)){reply(e,403);return;}
            if(origin!=null){e.getResponseHeaders().set("Access-Control-Allow-Origin",origin);e.getResponseHeaders().set("Vary","Origin");}
            if(e.getRequestMethod().equals("OPTIONS")){e.getResponseHeaders().set("Access-Control-Allow-Methods","POST");e.getResponseHeaders().set("Access-Control-Allow-Headers","Authorization, Content-Type");e.getResponseHeaders().set("Access-Control-Allow-Private-Network","true");e.getResponseHeaders().set("Access-Control-Max-Age","600");reply(e,204);return;}
            if(!e.getRequestMethod().equals("POST")){reply(e,405);return;}
            String auth=e.getRequestHeaders().getFirst("Authorization"),token=Aeternum.config.spotifyPairToken;
            if(token.isBlank()||auth==null||!MessageDigest.isEqual(auth.getBytes(StandardCharsets.UTF_8),("Bearer "+token).getBytes(StandardCharsets.UTF_8))){reply(e,401);return;}
            byte[] bytes=e.getRequestBody().readNBytes(16385);if(bytes.length>16384){reply(e,413);return;}
            accept(JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject());reply(e,204);
        }catch(Exception ex){reply(e,400);}finally{e.close();}
    }
    private static void reply(HttpExchange e,int status)throws java.io.IOException{e.sendResponseHeaders(status,-1);}
}
