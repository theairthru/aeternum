package dev.aeternum;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final Module module;
    private int page;
    private KeyMapping listening;
    private final List<Consumer<Integer>> rows=new ArrayList<>();
    private int x,w;
    private String feedback="";
    public SettingsScreen(Screen parent,Module m){super(Component.literal(m.title));this.parent=parent;module=m;}
    @Override protected void init(){
        clearWidgets();rows.clear();x=Math.max(16,(width-490)/2);w=Math.min(490,width-32);
        Config c=Aeternum.config;
        button("Module: "+(module.enabled()?"ON":"OFF"),()->{module.toggle();rebuildWidgets();});
        key(module==Module.FREELOOK||module==Module.ZOOM?"Hold key":"Toggle key",module.key);
        if(module.overlay){slider("Overlay scale",.5,3,()->module.state().scale,v->module.state().scale=v);if(module!=Module.ARMOUR)slider("Overlay opacity",.05,.95,()->c.opacity,v->c.opacity=v);button("Move this overlay",()->minecraft.setScreen(new HudEditor(this)));}
        switch(module){
            case ARMOUR->{button("Orientation: "+(c.armourVertical?"Vertical":"Horizontal"),()->{c.armourVertical=!c.armourVertical;rebuildWidgets();});slider("Low durability (%)",1,99,()->c.armourWarning,v->c.armourWarning=(int)Math.round(v));}
            case CLOCK->{button("Clock: "+(c.analogueClock?"Analogue":"Digital"),()->{c.analogueClock=!c.analogueClock;rebuildWidgets();});button("Time format: "+(c.clock24h?"24 hour":"12 hour"),()->{c.clock24h=!c.clock24h;rebuildWidgets();});}
            case LOW_HEALTH->{slider("Health points (2 = 1 heart)",1,40,()->c.healthThreshold,v->c.healthThreshold=(int)Math.round(v));slider("Sound repeat (seconds)",1,60,()->c.warningCooldown,v->c.warningCooldown=(int)Math.round(v));field("Warning text",c.warningText,v->{c.warningText=v;return true;});}
            case FREECAM->slider("Flight speed",.1,5,()->c.freecamSpeed,v->c.freecamSpeed=v);
            case ZOOM->{slider("Magnification (also mouse wheel)",1.5,12,()->c.zoomFactor,v->c.zoomFactor=v);button("Cinematic camera: "+(c.cinematicZoom?"ON":"OFF"),()->{c.cinematicZoom=!c.cinematicZoom;rebuildWidgets();});}
            case XRAY->{slider("Hidden block opacity",0,1,()->c.xrayOpacity,v->c.xrayOpacity=v);button("Choose visible blocks ("+c.xrayBlocks.size()+")",()->minecraft.setScreen(new BlockScreen(this)));}
            case HITBOXES->{button("Through walls: "+(c.hitboxesThroughWalls?"ON":"OFF"),()->{c.hitboxesThroughWalls=!c.hitboxesThroughWalls;rebuildWidgets();});for(String name:c.boxes.keySet()){Config.Box box=c.boxes.get(name);button(name+": "+(box.enabled?"ON":"OFF"),()->{box.enabled=!box.enabled;rebuildWidgets();});field(name+" colour (hex)",box.colour,v->{if(!v.matches("#?[0-9a-fA-F]{6}"))return false;box.colour=v;return true;});slider(name+" line width",1,6,()->box.width,v->box.width=v);}}
            case PERFORMANCE->{slider("Entity render distance",16,256,()->c.entityDistance,v->c.entityDistance=(int)Math.round(v));button("Occlusion culling: "+(c.occlusionCulling?"ON":"OFF"),()->{c.occlusionCulling=!c.occlusionCulling;rebuildWidgets();});button("Include players: "+(c.cullPlayers?"ON":"OFF"),()->{c.cullPlayers=!c.cullPlayers;rebuildWidgets();});}
            case CAPE->button("Open cape painter / import image",()->minecraft.setScreen(new CapeScreen(this)));
            case LYRICS->{button("Spicy Lyrics / Spicetify setup",()->minecraft.setScreen(new SpicySettingsScreen(this)));button("Display: "+(c.lyricsSingleLine?"Current line only":"Current and next line"),()->{c.lyricsSingleLine=!c.lyricsSingleLine;rebuildWidgets();});button("View full lyrics",()->minecraft.setScreen(new LyricsScreen(this)));button("Online lyrics: "+(c.mediaLyrics?"ON":"OFF"),()->{c.mediaLyrics=!c.mediaLyrics;rebuildWidgets();});slider("Lyrics timing offset (seconds)",-10,10,()->c.lyricsOffset,v->c.lyricsOffset=v);}
            case MEDIA->{key("Restart / previous",Aeternum.previous);key("Next track",Aeternum.next);key("Play / pause",Aeternum.play);button("Test play / pause",()->Aeternum.media.command("toggle"));}
            default->{}
        }

        int per=Math.max(1,(height-113)/39),pages=Math.max(1,(rows.size()+per-1)/per);page=Math.clamp(page,0,pages-1);
        for(int i=page*per;i<Math.min(rows.size(),(page+1)*per);i++)rows.get(i).accept(65+(i-page*per)*39);
        addRenderableWidget(new UiButton(x,height-27,58,20,"<",b->{page=(page+pages-1)%pages;rebuildWidgets();}));
        addRenderableWidget(new UiButton(x+64,height-27,88,20,(page+1)+" / "+pages,b->{page=(page+1)%pages;rebuildWidgets();}));
        addRenderableWidget(new UiButton(x+w-94,height-27,94,20,"Back",b->onClose()));
    }
    private void button(String label,Runnable action){rows.add(y->addRenderableWidget(new UiButton(x,y,w,26,label,b->action.run())));}
    private void key(String label,KeyMapping key){button(label+": "+key.getTranslatedKeyMessage().getString(),()->{listening=key;feedback="Press a key or mouse button. Esc cancels; Delete clears.";});}
    private void field(String label,String value,Predicate<String> setter){rows.add(y->{EditBox f=new EditBox(font,x,y+12,w,20,Component.literal(label));f.setMaxLength(160);f.setValue(value);f.setResponder(s->{boolean valid=setter.test(s);f.setTextColor(valid?0xffeeeeee:0xffff657d);});addRenderableWidget(f);addRenderableOnly((g,mx,my,d)->g.drawString(font,label,x,y,0xffaaaaaa,false));});}
    private void slider(String label,double min,double max,DoubleSupplier get,DoubleConsumer set){rows.add(y->addRenderableWidget(new AbstractSliderButton(x,y,w,26,Component.empty(),(get.getAsDouble()-min)/(max-min)){
        {updateMessage();}
        protected void updateMessage(){setMessage(Component.literal(label+": "+String.format(Locale.ROOT,"%.2f",min+value*(max-min))));}
        protected void applyValue(){set.accept(min+value*(max-min));}
    }));}
    private void bind(InputConstants.Key key){
        listening.setKey(key);KeyMapping.resetMapping();minecraft.options.save();Aeternum.saveBindings();listening=null;
        long conflicts=java.util.stream.Stream.concat(Arrays.stream(minecraft.options.keyMappings),Aeternum.privateBindings.values().stream()).filter(k->k.saveString().equals(key.getName())).count();feedback=conflicts>1&&key!=InputConstants.UNKNOWN?"This key is shared with another control.":"Key saved.";rebuildWidgets();
    }
    @Override public boolean keyPressed(KeyEvent e){if(listening!=null){if(e.key()==GLFW.GLFW_KEY_ESCAPE){listening=null;feedback="Binding cancelled.";}else bind(e.key()==GLFW.GLFW_KEY_DELETE?InputConstants.UNKNOWN:InputConstants.Type.KEYSYM.getOrCreate(e.key()));return true;}return super.keyPressed(e);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean dbl){if(listening!=null){bind(InputConstants.Type.MOUSE.getOrCreate(e.button()));return true;}return super.mouseClicked(e,dbl);}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float d){ClientScreen.backdrop(g,width,height);g.drawString(font,module.title,x,17,0xffeeeeee,false);g.drawString(font,font.plainSubstrByWidth(module.description,w),x,33,0xffaaaaaa,false);super.render(g,mx,my,d);g.drawString(font,font.plainSubstrByWidth(feedback,w),x,height-41,0xffcccccc,false);}
    @Override public void removed(){Aeternum.config.save();if(module==Module.XRAY){Xray.refresh();minecraft.levelRenderer.allChanged();}}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}





