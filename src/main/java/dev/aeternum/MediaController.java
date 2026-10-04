package dev.aeternum;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import net.fabricmc.loader.api.FabricLoader;

/** All OS calls run off the render thread, with bounded process lifetime. */
public final class MediaController {
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Aeternum media");t.setDaemon(true);return t;});
    private final java.util.concurrent.atomic.AtomicBoolean busy=new java.util.concurrent.atomic.AtomicBoolean();
    private volatile long nextPoll;
    public volatile String title="Media ready",artist="Open your music or video player";
    public volatile String album="";
    public volatile double duration,elapsed;
    public volatile boolean playing,available;
    public final LyricsController lyrics=new LyricsController();
    private volatile Process bridge;
    private java.io.BufferedReader bridgeOutput;
    private java.io.BufferedWriter bridgeInput;
    public MediaController(){Runtime.getRuntime().addShutdownHook(new Thread(this::stopBridge,"Aeternum media shutdown"));}
    public void stopBridge(){Process current=bridge;if(current!=null)current.destroy();}
    public volatile long measuredAt;
    public net.minecraft.resources.Identifier art;
    private volatile String lastArt="";
    private String lastArtError="";
    private final PlaybackClock clock=new PlaybackClock();
    public double position(){if(clock.initialized())return clock.position(System.nanoTime());return rawPosition();}
    private double rawPosition(){double p=elapsed+(playing?(System.currentTimeMillis()-measuredAt)/1000d:0);return duration>0?Math.clamp(p,0,duration):Math.max(0,p);}
    private Path script;
    public void poll() {
        SpotifyBridge.ensureStarted();
        var spotify=SpotifyBridge.current();
        if(Module.LYRICS.enabled()&&Aeternum.config.mediaLyrics){
            if(spotify!=null)lyrics.observe(spotify.title(),spotify.artist(),spotify.album(),spotify.duration(),spotify.id());
            else if(available)lyrics.observe(title,artist,album,duration);else lyrics.clear();
        }else lyrics.clear();
        if(System.currentTimeMillis()>nextPoll){nextPoll=System.currentTimeMillis()+500;command("status");}
    }
    public double lyricsPosition(){return SpotifyBridge.current()!=null?SpotifyBridge.position():position();}
    public void command(String action) {
        boolean polling=action.equals("status");if(polling&&!busy.compareAndSet(false,true))return;
        worker.submit(()->{try {
            String os=System.getProperty("os.name").toLowerCase();
            if(os.contains("win"))windows(action);
            else if(os.contains("linux"))linux(action);
            else {title="Media unavailable";artist="Windows or Linux required";}
        }catch(Exception e){unavailable();nextPoll=System.currentTimeMillis()+5000;stopBridge();Aeternum.LOG.debug("Media bridge",e);}finally{if(polling)busy.set(false);}});
    }
    private void windows(String action)throws Exception {
        if(script==null) {
            Path dir=FabricLoader.getInstance().getConfigDir().resolve("aeternum");Files.createDirectories(dir);script=dir.resolve("media.ps1");
            try(var in=getClass().getResourceAsStream("/assets/aeternum/media.ps1")){Files.copy(in,script,StandardCopyOption.REPLACE_EXISTING);}
        }
        if(bridge==null||!bridge.isAlive()){
            bridge=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-ExecutionPolicy","Bypass","-File",script.toString(),"serve").redirectError(ProcessBuilder.Redirect.DISCARD).start();
            bridgeOutput=bridge.inputReader(StandardCharsets.UTF_8);bridgeInput=bridge.outputWriter(StandardCharsets.UTF_8);
        }
        bridgeInput.write(action);bridgeInput.newLine();bridgeInput.flush();
        var reader=bridgeOutput;
        String result=CompletableFuture.supplyAsync(()->{try{return reader.readLine();}catch(Exception e){throw new CompletionException(e);}}).get(6,TimeUnit.SECONDS);
        if(result==null)throw new IllegalStateException("Media helper closed");
        acceptSnapshot(JsonParser.parseString(result.strip()).getAsJsonObject());
    }
    void acceptSnapshot(com.google.gson.JsonObject j){
        if(j.has("available")&&!j.get("available").getAsBoolean()){unavailable();return;}
        String track=j.get("title").getAsString()+"\n"+j.get("artist").getAsString()+"\n"+j.get("duration").getAsString();
        long stamp=j.has("measuredAt")?j.get("measuredAt").getAsLong():System.currentTimeMillis();
        boolean active=j.get("playing").getAsBoolean();
        double incoming=j.get("position").getAsDouble()+(active?Math.clamp((System.currentTimeMillis()-stamp)/1000d,0,2):0);
        clock.update(track,incoming,j.get("duration").getAsDouble(),active,System.nanoTime());
        available=true;title=j.get("title").getAsString();artist=j.get("artist").getAsString();
        album=j.get("album").getAsString();duration=j.get("duration").getAsDouble();elapsed=j.get("position").getAsDouble();playing=j.get("playing").getAsBoolean();
        measuredAt=j.has("measuredAt")?j.get("measuredAt").getAsLong():System.currentTimeMillis();
        String artError=j.has("artError")?j.get("artError").getAsString():"";
        if(!artError.equals(lastArtError)){lastArtError=artError;if(!artError.isBlank())Aeternum.LOG.warn("Media thumbnail could not be read: {}",artError);}
        if(j.has("art"))updateArt(j.get("art").getAsString());
    }
    private void unavailable(){clock.reset();available=false;title="No media session";artist="Open your music or video player";album="";duration=elapsed=0;playing=false;updateArt("");lyrics.clear();}    private void linux(String action)throws Exception {
        switch(action) {
            case "next"->run("playerctl","next");
            case "toggle"->run("playerctl","play-pause");
            case "previous"->{double p=Double.parseDouble(run("playerctl","position").strip());if(p<=3)run("playerctl","previous");else run("playerctl","position","0");}
        }
        available=true;
        title=run("playerctl","metadata","--format","{{title}}").strip();artist=run("playerctl","metadata","--format","{{artist}}").strip();
        album=run("playerctl","metadata","--format","{{album}}").strip();
        String length=run("playerctl","metadata","--format","{{mpris:length}}").strip();duration=length.isBlank()?0:Double.parseDouble(length)/1e6;
        elapsed=Double.parseDouble(run("playerctl","position").strip());playing=run("playerctl","status").strip().equals("Playing");measuredAt=System.currentTimeMillis();
        String uri=run("playerctl","metadata","--format","{{mpris:artUrl}}").strip();
        if(uri.startsWith("file:")){Path path=Path.of(java.net.URI.create(uri));if(Files.size(path)<=2_000_000)updateArt(java.util.Base64.getEncoder().encodeToString(Files.readAllBytes(path)));else updateArt("");}else updateArt("");
    }
    private void updateArt(String encoded){
        if(encoded.equals(lastArt))return;
        byte[] png=null;
        if(!encoded.isBlank())try {
            byte[] bytes=java.util.Base64.getDecoder().decode(encoded);if(bytes.length>8_388_608)throw new IllegalArgumentException("Thumbnail exceeds 8 MB");
            var source=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if(source==null)throw new IllegalArgumentException("Unsupported thumbnail image format");
            var scaled=roundedCover(source);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(scaled,"png",out);png=out.toByteArray();
        }catch(Exception e){Aeternum.LOG.debug("Album art unavailable; will retry",e);}
        byte[] image=png;
        net.minecraft.client.Minecraft.getInstance().execute(()->{
            var textures=net.minecraft.client.Minecraft.getInstance().getTextureManager();if(art!=null){textures.release(art);art=null;}
            if(image!=null)try{var id=Aeternum.id("media-art");textures.register(id,new net.minecraft.client.renderer.texture.DynamicTexture(()->"Aeternum album art",com.mojang.blaze3d.platform.NativeImage.read(new java.io.ByteArrayInputStream(image))));art=id;lastArt=encoded;}catch(Exception e){Aeternum.LOG.debug("Album texture unavailable; will retry",e);}
            else lastArt="";
        });
    }
    static java.awt.image.BufferedImage roundedCover(java.awt.image.BufferedImage source){
        var result=new java.awt.image.BufferedImage(128,128,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g=result.createGraphics();g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source,0,0,128,128,null);g.dispose();
        // Coverage mask is baked once per cover, never rebuilt during HUD rendering.
        double radius=16;
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            double dx=Math.max(Math.abs(x+.5-64)-(64-radius),0),dy=Math.max(Math.abs(y+.5-64)-(64-radius),0);
            double coverage=Math.clamp(radius+.5-Math.hypot(dx,dy),0,1);
            int pixel=result.getRGB(x,y);result.setRGB(x,y,((int)((pixel>>>24)*coverage)<<24)|(pixel&0xffffff));
        }
        return result;
    }    private static String run(String...command)throws Exception {
        Process p=new ProcessBuilder(command).redirectErrorStream(true).start();
        var out=CompletableFuture.supplyAsync(()->{try{return new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);}catch(Exception e){return "";}});
        if(!p.waitFor(6,TimeUnit.SECONDS)){p.destroyForcibly();throw new IllegalStateException("Media timeout");}
        String s=out.get(1,TimeUnit.SECONDS);if(p.exitValue()!=0)throw new IllegalStateException(s);return s;
    }
}


