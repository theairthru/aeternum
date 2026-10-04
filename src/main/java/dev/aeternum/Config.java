package dev.aeternum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

public final class Config {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aeternum.json");
    public Map<String,String> bindings=new LinkedHashMap<>();
    public boolean cinematicZoom=true,mediaLyrics=true;
    public boolean lyricsSingleLine=false,musixmatchFallback=true;
    public String musixmatchApiKey="",spicyApiKey="",spotifyPairToken="";
    public boolean spotifyBridgeEnabled=false;
    public double lyricsOffset=0;
    public Map<String,State> modules = new LinkedHashMap<>();
    public int armourWarning=15, healthThreshold=6, warningCooldown=8;
    public String warningText="LOW HEALTH — heal now!";
    public boolean analogueClock=false, clock24h=true, armourVertical=false, snapping=true;
    public double opacity=.55, freecamSpeed=.7, zoomFactor=4, xrayOpacity=.12;
    public int entityDistance=96;
    public boolean occlusionCulling=true, cullPlayers=false;
    public boolean hitboxesThroughWalls=false;
    public List<String> xrayBlocks=new ArrayList<>(List.of("minecraft:diamond_ore","minecraft:deepslate_diamond_ore","minecraft:ancient_debris","minecraft:emerald_ore","minecraft:deepslate_emerald_ore"));
    public Map<String,Box> boxes=new LinkedHashMap<>();
    public static final class State {
        public boolean enabled;
        public double x=.02,y=.04,scale=1;
        public State() {}
        State(boolean e,double x,double y) { enabled=e; this.x=x; this.y=y; }
    }
    public static final class Box {
        public boolean enabled=true;
        public String colour="69B8FF";
        public double width=2;
        public Box() {}
        Box(String c) { colour=c; }
        public int rgb() { try { return Integer.parseInt(colour.replace("#",""),16)&0xffffff; } catch(Exception e) { return 0x69b8ff; } }
    }
    public void normalize() {
        if(spicyApiKey==null)spicyApiKey="";if(spotifyPairToken==null)spotifyPairToken="";
        if(musixmatchApiKey==null)musixmatchApiKey="";
        if(bindings==null)bindings=new LinkedHashMap<>();
        if(modules==null) modules=new LinkedHashMap<>();
        int i=0;
        for(Module m:Module.values()) {
            double x=.02,y=.04+i*.065;
            if(m==Module.ARMOUR) {x=.5;y=.83;}
            if(m==Module.LOW_HEALTH) {x=.5;y=.78;}
            if(m==Module.KEYSTROKES) {x=.86;y=.65;}
            if(m==Module.CLOCK) {x=.88;y=.04;}
            if(m==Module.FPS) {x=.02;y=.04;}
            if(m==Module.PING) {x=.02;y=.11;}
            if(m==Module.COORDINATES) {x=.02;y=.18;}
            if(m==Module.LYRICS) {x=.5;y=.65;}
            if(m==Module.MEDIA) {x=.02;y=.88;}
            if(modules.get(m.id())==null) modules.put(m.id(),new State(m==Module.LYRICS?mediaLyrics:m.defaultEnabled,x,y));
            State s=modules.get(m.id()); s.x=clamp(s.x,0,1);s.y=clamp(s.y,0,1);s.scale=clamp(s.scale,.5,3);
            if(m.overlay)i++;
        }
        if(boxes==null)boxes=new LinkedHashMap<>();
        boxes.putIfAbsent("Players",new Box("69B8FF")); boxes.putIfAbsent("Mobs",new Box("FFB86B"));
        boxes.putIfAbsent("Projectiles",new Box("F18DFF")); boxes.putIfAbsent("Items",new Box("73E8B3")); boxes.putIfAbsent("Other",new Box("C5D2E3"));
        lyricsOffset=clamp(lyricsOffset,-10,10);
        opacity=clamp(opacity,.05,.95); xrayOpacity=clamp(xrayOpacity,0,1); freecamSpeed=clamp(freecamSpeed,.1,5);zoomFactor=clamp(zoomFactor,1.5,12);
        armourWarning=Math.clamp(armourWarning,1,99);healthThreshold=Math.clamp(healthThreshold,1,40);warningCooldown=Math.clamp(warningCooldown,1,60);entityDistance=Math.clamp(entityDistance,16,256);
        if(warningText==null)warningText="LOW HEALTH";
        if(xrayBlocks==null)xrayBlocks=new ArrayList<>();
    }
    static double clamp(double v,double min,double max) {return Double.isFinite(v)?Math.clamp(v,min,max):min;}
    public static Config load() {
        Config c=null;
        try { if(Files.exists(FILE))c=JSON.fromJson(Files.readString(FILE),Config.class); }
        catch(Exception e) { Aeternum.LOG.warn("Could not read config; keeping a backup",e);try{Files.copy(FILE,FILE.resolveSibling("aeternum.broken.json"),StandardCopyOption.REPLACE_EXISTING);}catch(Exception ignored){} }
        if(c==null)c=new Config();c.normalize();return c;
    }
    public void save() {
        try { Files.createDirectories(FILE.getParent());Path temp=FILE.resolveSibling("aeternum.json.tmp");Files.writeString(temp,JSON.toJson(this));
            try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        } catch(Exception e) {Aeternum.LOG.error("Could not save Aeternum settings",e);}
    }
}


