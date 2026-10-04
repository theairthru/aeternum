package dev.aeternum;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.*;

public final class Aeternum implements ClientModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("Aeternum");
    public static Config config;
    public static KeyMapping menu,previous,next,play;
    public static final java.util.Map<String,KeyMapping> privateBindings=new java.util.LinkedHashMap<>();
    private static Boolean previousSmooth;
    public static boolean zooming(){return config!=null&&Module.ZOOM.enabled()&&Module.ZOOM.key.isDown()&&Minecraft.getInstance().screen==null;}
    public static void saveBindings(){privateBindings.forEach((name,key)->config.bindings.put(name,key.saveString()));config.save();}
    public static boolean lowHealth;
    public static int renderedHitboxes;
    private static long lastWarning;
    private static boolean wasLow;
    public static final CameraController camera=new CameraController();
    public static final MediaController media=new MediaController();
    public static Identifier id(String s) {return Identifier.fromNamespaceAndPath("aeternum",s);}
    public void onInitializeClient() {
        config=Config.load();
        KeyMapping.Category category=KeyMapping.Category.register(id("controls"));
        menu=bind("menu",GLFW.GLFW_KEY_RIGHT_SHIFT,category);
        for(Module m:Module.values()) {
            int key=switch(m) {case FREELOOK->GLFW.GLFW_KEY_LEFT_ALT;case FREECAM->GLFW.GLFW_KEY_F6;case ZOOM->GLFW.GLFW_KEY_C;default->GLFW.GLFW_KEY_UNKNOWN;};
            m.key=bind(m.id(),key,category);
        }
        previous=bind("media_previous",GLFW.GLFW_KEY_LEFT_BRACKET,category);
        next=bind("media_next",GLFW.GLFW_KEY_RIGHT_BRACKET,category);
        // GLFW names the physical UK # key BACKSLASH. All keys remain rebindable.
        play=bind("media_play",GLFW.GLFW_KEY_BACKSLASH,category);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,id("hud"),(g,t)->Hud.render(g,false));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        Xray.refresh();
        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(context->context.modifyBlockModelAfterBake().register((model,ctx)->new XrayModel(model)));
    }
    private static KeyMapping bind(String name,int key,KeyMapping.Category category) {
        KeyMapping binding=new KeyMapping("key.aeternum."+name,InputConstants.Type.KEYSYM,key,category);
        if(name.equals("menu"))return KeyBindingHelper.registerKeyBinding(binding);
        String saved=config.bindings.get(name);
        if(saved!=null)try{binding.setKey(InputConstants.getKey(saved));}catch(Exception ignored){}
        privateBindings.put(name,binding);KeyMapping.resetMapping();return binding;
    }
    private void tick(Minecraft mc) {
        boolean smooth=zooming()&&config.cinematicZoom&&mc.player!=null;
        if(smooth&&previousSmooth==null){previousSmooth=mc.options.smoothCamera;mc.options.smoothCamera=true;}
        if(!smooth&&previousSmooth!=null){mc.options.smoothCamera=previousSmooth;previousSmooth=null;}
        while(menu.consumeClick()) if(mc.screen==null)mc.setScreen(new ClientScreen());
        if(mc.level==null||mc.player==null) {camera.reset();lowHealth=false;wasLow=false;return;}
        if(mc.screen==null) {
            for(Module m:Module.values()) {
                if(m==Module.ZOOM||m==Module.FREELOOK)continue;
                while(m.key.consumeClick())m.toggle();
            }
            if(Module.MEDIA.enabled()) {
                while(previous.consumeClick())media.command("previous");
                while(next.consumeClick())media.command("next");
                while(play.consumeClick())media.command("toggle");
            }
        } else {
            for(Module m:Module.values())while(m.key.consumeClick()){}
            while(previous.consumeClick()){}while(next.consumeClick()){}while(play.consumeClick()){}
        }
        camera.tick(mc);
        if(Module.SPRINT.enabled()&&mc.screen==null&&!camera.freecamActive&&mc.options.keyUp.isDown()&&!mc.player.isShiftKeyDown())mc.player.setSprinting(true);
        lowHealth=Module.LOW_HEALTH.enabled()&&mc.player.isAlive()&&mc.player.getHealth()<=config.healthThreshold;
        long now=System.currentTimeMillis();
        if(lowHealth&&(!wasLow||now-lastWarning>=config.warningCooldown*1000L)) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(),.7f,1.4f);lastWarning=now;
        }
        wasLow=lowHealth;
        if(Module.MEDIA.enabled()||Module.LYRICS.enabled())media.poll();
        Culling.tick();
    }
    public static void changed(Module m) {
        config.save();
        if(m==Module.XRAY) {Xray.refresh();Minecraft.getInstance().levelRenderer.allChanged();}
        if(m==Module.FREECAM&&!m.enabled())camera.reset();
    }
}
