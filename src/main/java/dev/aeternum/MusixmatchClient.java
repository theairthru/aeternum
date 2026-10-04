package dev.aeternum;

import com.google.gson.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/** Official API only. No embedded credentials, browser cookies, or API-key logging. */
final class MusixmatchClient {
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private long blockedUntil,lastRequest;
    private static LyricsController.Result message(String s,boolean retry){return new LyricsController.Result(List.of(),List.of(),s,retry);}
    LyricsController.Result lookup(String title,String artist,double duration,String key){
        if(System.currentTimeMillis()<blockedUntil)return message("Musixmatch limit; retrying later",true);
        try{
            var body=request("matcher.track.get?q_track="+encode(title)+"&q_artist="+encode(artist),key);
            if(body==null||!body.has("track"))return message("No lyrics found",false);
            var track=body.getAsJsonObject("track");
            if(number(track,"restricted")==1)return message("Musixmatch: lyrics restricted",false);
            if(number(track,"instrumental")==1)return message("Instrumental",false);
            if(duration>0&&number(track,"track_length")>0&&Math.abs(number(track,"track_length")-duration)>5)return message("Musixmatch version does not match",false);
            String id=track.get("track_id").getAsString();
            JsonObject subtitles=null;
            try{subtitles=request("track.subtitle.get?track_id="+encode(id)+"&subtitle_format=lrc",key);}catch(ApiError e){if(e.status!=403&&e.status!=404)throw e;}
            if(subtitles!=null&&subtitles.has("subtitle")){
                var sub=subtitles.getAsJsonObject("subtitle");
                var result=parse(sub,true);if(!result.timed().isEmpty())return result;
            }
            var lyrics=request("track.lyrics.get?track_id="+encode(id),key);
            return lyrics!=null&&lyrics.has("lyrics")?parse(lyrics.getAsJsonObject("lyrics"),false):message("No lyrics found",false);
        }catch(ApiError e){return switch(e.status){
            case 401->message("Musixmatch: check your API key",false);
            case 403->message("Musixmatch: access not included",false);
            case 402,429->message("Musixmatch limit; retrying later",true);
            default->message("Musixmatch unavailable; retrying",true);
        };}catch(Exception e){return message("Musixmatch unavailable; retrying",true);}
    }
    static LyricsController.Result parse(JsonObject object,boolean synced){
        if(number(object,"restricted")==1)return message("Musixmatch: lyrics restricted",false);
        String words=text(object,synced?"subtitle_body":"lyrics_body");
        String credit="Lyrics powered by Musixmatch\n"+text(object,synced?"subtitle_copyright":"lyrics_copyright");
        if(words.isBlank())return message("No lyrics found",false);
        return new LyricsController.Result(synced?LyricsController.parseLrc(words):List.of(),synced?List.of():Arrays.asList(words.split("\\R")),synced?"Musixmatch":"Unsynced lyrics · open Lyrics settings",false,credit);
    }
    private JsonObject request(String path,String key)throws Exception{
        long delay=350-(System.currentTimeMillis()-lastRequest);if(delay>0)Thread.sleep(delay);lastRequest=System.currentTimeMillis();
        var req=HttpRequest.newBuilder(URI.create("https://api.musixmatch.com/ws/1.1/"+path+"&apikey="+encode(key))).timeout(Duration.ofSeconds(10)).header("Accept","application/json").header("User-Agent","Aeternum/0.5.0").GET().build();
        var response=http.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status=response.statusCode();
        if(status==200&&response.body().length()<=1_000_000){
            var envelope=JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("message");status=envelope.getAsJsonObject("header").get("status_code").getAsInt();
            if(status==200)return envelope.getAsJsonObject("body");
        }
        if(status==404)return null;
        if(status==402||status==429){long wait=300000;try{wait=Math.max(wait,Long.parseLong(response.headers().firstValue("Retry-After").orElse("300"))*1000);}catch(Exception ignored){}blockedUntil=System.currentTimeMillis()+wait;}
        throw new ApiError(status);
    }
    private static final class ApiError extends Exception{final int status;ApiError(int status){super("Musixmatch response "+status);this.status=status;}}
    private static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    private static String text(JsonObject j,String k){return j.has(k)&&!j.get(k).isJsonNull()?j.get(k).getAsString():"";}
    private static double number(JsonObject j,String k){return j.has(k)&&!j.get(k).isJsonNull()?j.get(k).getAsDouble():0;}
}
