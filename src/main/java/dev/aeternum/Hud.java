package dev.aeternum;

import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Hud {
    private static final Minecraft MC=Minecraft.getInstance();
    public static final Map<Module,Rect> bounds=new EnumMap<>(Module.class);
    public static final Map<Module,Rect> baseBounds=new EnumMap<>(Module.class);
    private static final ArrayDeque<Long> left=new ArrayDeque<>(),right=new ArrayDeque<>();
    public record Rect(int x,int y,int w,int h) {public boolean contains(double a,double b){return a>=x&&b>=y&&a<x+w&&b<y+h;}}
    public static void click(int button){if(button==0)left.add(System.currentTimeMillis());if(button==1)right.add(System.currentTimeMillis());}
    private static int cps(ArrayDeque<Long> q){long t=System.currentTimeMillis()-1000;while(!q.isEmpty()&&q.peek()<t)q.remove();return q.size();}
    public static void render(GuiGraphics g,boolean edit) {
        if(Aeternum.config==null||MC.player==null)return;
        if(!edit&&MC.screen instanceof HudEditor)return;
        if(!edit)HitboxHud.render(g);
        bounds.clear();baseBounds.clear();Shapes.submittedQuads=0;
        for(Module m:Module.values())if(m.overlay&&m.enabled()) {
            g.pose().pushMatrix();try {switch(m) {
                case ARMOUR->armour(g,edit);
                case KEYSTROKES->keys(g);
                case CLOCK->clock(g);
                case FPS->panel(g,m,MC.getFps()+" FPS");
                case PING->{var info=MC.getConnection()==null?null:MC.getConnection().getPlayerInfo(MC.player.getUUID());panel(g,m,(info==null?"—":info.getLatency())+" ms");}
                case COORDINATES->panel(g,m,String.format(Locale.ROOT,"XYZ  %.1f  %.1f  %.1f",MC.player.getX(),MC.player.getY(),MC.player.getZ()),MC.player.getDirection().getName().toUpperCase(Locale.ROOT));
                case LOW_HEALTH->{if(Aeternum.lowHealth||edit)panel(g,m,"!  "+Aeternum.config.warningText);}
                case MEDIA->media(g);
                case LYRICS->lyrics(g);
                case EFFECTS->effects(g,edit);
                default->{}
            }}finally{g.pose().popMatrix();}
        }
    }
    private static String shorten(String s,int w){return MC.font.plainSubstrByWidth(s,w);}
    public static Rect rect(GuiGraphics g,Module m,int w,int h) {
        int sw=MC.getWindow().getGuiScaledWidth(),sh=MC.getWindow().getGuiScaledHeight();
        double scale=Math.min(m.state().scale,Math.min((double)sw/w,(double)sh/h));
        int rw=(int)Math.ceil(w*scale),rh=(int)Math.ceil(h*scale);
        int x=(int)Math.round(m.state().x*Math.max(0,sw-rw)),y=(int)Math.round(m.state().y*Math.max(0,sh-rh));
        bounds.put(m,new Rect(x,y,rw,rh));baseBounds.put(m,new Rect(0,0,w,h));
        g.pose().translate(x,y).scale((float)scale,(float)scale);
        return new Rect(0,0,w,h);
    }    private static void background(GuiGraphics g,Rect r){Shapes.round(g,r.x,r.y,r.w,r.h,7,((int)(255*Aeternum.config.opacity)<<24)|0x181818);}
    private static void text(GuiGraphics g,String s,int x,int y,int col){g.drawString(MC.font,s,x,y,col,true);}
    private static void panel(GuiGraphics g,Module m,String...lines){int w=24;for(String l:lines)w=Math.max(w,MC.font.width(l)+14);Rect r=rect(g,m,w,lines.length*12+8);background(g,r);for(int i=0;i<lines.length;i++)text(g,lines[i],r.x+7,r.y+5+i*12,m==Module.LOW_HEALTH?0xffff727d:i==0?0xffeeeeee:0xffaaaaaa);}
    private static void armour(GuiGraphics g,boolean edit) {
        boolean vertical=Aeternum.config.armourVertical;
        Rect r=rect(g,Module.ARMOUR,vertical?24:84,vertical?94:34);
        EquipmentSlot[] slots={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
        Item[] demo={Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS};
        for(int i=0;i<4;i++){
            int x=r.x+(vertical?0:i*20),y=r.y+10+(vertical?i*20:0);
            ItemStack stack=MC.player.getItemBySlot(slots[i]);if(edit&&stack.isEmpty())stack=new ItemStack(demo[i]);
            if(stack.isEmpty())continue;
            g.fill(x+1,y+1,x+21,y+21,0xff333333);
            // A crop of the actual vanilla hotbar sprite respects resource packs.
            g.blitSprite(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("hud/hotbar"),182,22,0,0,x,y,22,22,0xffffffff);
            if(!stack.isEmpty()){
                g.renderItem(stack,x+3,y+3);
                if(stack.isDamageableItem()) {
                    double fraction=1-(double)stack.getDamageValue()/stack.getMaxDamage();
                    g.nextStratum();
                    g.fill(x+3,y+18,x+19,y+20,0xff111111);g.fill(x+3,y+18,x+3+Math.max(1,(int)Math.round(16*fraction)),y+20,fraction*100<=Aeternum.config.armourWarning?0xffff465a:0xff73e8b3);
                    if(fraction*100<=Aeternum.config.armourWarning){text(g,"!",x+9,y-8,0xffff465a);}
                }
            }
        }
    }
    private static void keys(GuiGraphics g) {
        Rect r=rect(g,Module.KEYSTROKES,80,88);
        key(g,r.x+27,r.y,26,MC.options.keyUp.getTranslatedKeyMessage().getString(),MC.options.keyUp.isDown());
        key(g,r.x,r.y+27,26,MC.options.keyLeft.getTranslatedKeyMessage().getString(),MC.options.keyLeft.isDown());
        key(g,r.x+27,r.y+27,26,MC.options.keyDown.getTranslatedKeyMessage().getString(),MC.options.keyDown.isDown());
        key(g,r.x+54,r.y+27,26,MC.options.keyRight.getTranslatedKeyMessage().getString(),MC.options.keyRight.isDown());
        key(g,r.x,r.y+54,39,"L "+cps(left),MC.options.keyAttack.isDown());key(g,r.x+41,r.y+54,39,"R "+cps(right),MC.options.keyUse.isDown());
        g.fill(r.x,r.y+81,r.x+80,r.y+88,MC.options.keyJump.isDown()?0xb0cccccc:((int)(255*Aeternum.config.opacity)<<24)|0x181818);
    }
    private static void key(GuiGraphics g,int x,int y,int w,String s,boolean down){g.fill(x,y,x+w,y+25,down?0xb0888888:((int)(255*Aeternum.config.opacity)<<24)|0x181818);String label=shorten(s,w-4);text(g,label,x+(w-MC.font.width(label))/2,y+9,0xffeeeeee);}
    private static void clock(GuiGraphics g) {
        LocalTime now=LocalTime.now();
        if(!Aeternum.config.analogueClock){panel(g,Module.CLOCK,now.format(DateTimeFormatter.ofPattern(Aeternum.config.clock24h?"HH:mm:ss":"hh:mm:ss a")));return;}
        Rect r=rect(g,Module.CLOCK,66,66);background(g,r);int cx=r.x+33,cy=r.y+33;
        g.blit(Shapes.DIAL,r.x,r.y,r.x+66,r.y+66,0,1,0,1);
        double seconds=now.getSecond()+now.getNano()/1e9;
        hand(g,cx,cy,(now.getHour()%12+(now.getMinute()+seconds/60)/60)/12,15,2.1,0xffeeeeee);
        hand(g,cx,cy,(now.getMinute()+seconds/60)/60,23,1.4,0xffeeeeee);
        hand(g,cx,cy,seconds/60,26,.7,0xffaaaaaa);
        Shapes.stroke(g,cx,cy,cx,cy,3,0xffffffff);
    }
    private static void hand(GuiGraphics g,int x,int y,double turn,int len,double width,int color){double a=turn*2*Math.PI;Shapes.stroke(g,x,y,x+Math.sin(a)*len,y-Math.cos(a)*len,width,color);}
    private static void effects(GuiGraphics g,boolean edit){
        var effects=MC.player.getActiveEffects();if(effects.isEmpty()){if(edit)panel(g,Module.EFFECTS,"No active effects");return;}
        java.util.List<String> lines=new ArrayList<>();int w=100;
        for(var e:effects){int sec=e.getDuration()/20;String s=e.getEffect().value().getDisplayName().getString()+" "+(e.getAmplifier()+1)+"  "+(e.isInfiniteDuration()?"∞":String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60));lines.add(s);w=Math.max(w,MC.font.width(s)+35);}
        Rect r=rect(g,Module.EFFECTS,w,effects.size()*22+8);background(g,r);int i=0;
        for(var e:effects){var id=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());g.blitSprite(RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath(id.getNamespace(),"mob_effect/"+id.getPath()),r.x+5,r.y+5+i*22,18,18);text(g,lines.get(i),r.x+28,r.y+10+i*22,0xffeeeeee);i++;}
    }
    private static void media(GuiGraphics g){
        var m=Aeternum.media;Rect r=rect(g,Module.MEDIA,244,83);background(g,r);
        if(m.art!=null)g.blit(m.art,r.x+7,r.y+7,r.x+55,r.y+55,0,1,0,1);
        else {Shapes.round(g,r.x+7,r.y+7,48,48,6,0x90444444);text(g,"♪",r.x+26,r.y+26,0xffdddddd);}
        text(g,shorten(m.title,173),r.x+63,r.y+17,0xffeeeeee);
        text(g,shorten(m.artist,173),r.x+63,r.y+33,0xffcccccc);

        double p=m.position();int fill=m.duration>0?(int)(230*Math.clamp(p/m.duration,0,1)):0;
        Shapes.round(g,r.x+7,r.y+60,230,3,1,0xff555555);if(fill>0)Shapes.round(g,r.x+7,r.y+60,fill,3,1,0xffdddddd);
        text(g,time(p)+" / "+(m.duration>0?time(m.duration):"--:--"),r.x+7,r.y+69,0xffbbbbbb);
        String state=m.playing?"Playing":"Paused";text(g,state,r.x+r.w-7-MC.font.width(state),r.y+69,0xff999999);
    }
    private static int lyricResult;
    private static double lyricRow;
    private static long lyricFrame;
    private static void lyrics(GuiGraphics g){
        var result=Aeternum.media.lyrics.result;var lines=result.timed();
        boolean single=Aeternum.config.lyricsSingleLine;
        int bodyH=single?26:46;var credits=LyricsCredits.rows(result,230);
        Rect r=rect(g,Module.LYRICS,244,bodyH+credits.size()*11);background(g,r);
        for(int i=0;i<credits.size();i++)g.drawString(MC.font,credits.get(i).text(),7,bodyH+i*11-2,0xffaaaaaa,true);
        if(lines.isEmpty()){text(g,shorten(result.notice(),230),7,9,0xffcccccc);return;}
        double pos=Aeternum.media.lyricsPosition()+Aeternum.config.lyricsOffset;
        int index=-1;for(int i=0;i<lines.size()&&lines.get(i).seconds()<=pos;i++)index=i;
        long now=System.nanoTime();double dt=Math.min(.1,Math.max(0,(now-lyricFrame)/1e9));lyricFrame=now;
        if(System.identityHashCode(result)!=lyricResult||Math.abs(index-lyricRow)>2){lyricRow=index;lyricResult=System.identityHashCode(result);}
        else {lyricRow+=(index-lyricRow)*(1-Math.exp(-dt*13));if(Math.abs(index-lyricRow)<.001)lyricRow=index;}
        g.enableScissor(5,5,239,bodyH-5);
        for(int i=Math.max(-1,(int)Math.floor(lyricRow)-1);i<=Math.min(lines.size()-1,(int)Math.ceil(lyricRow)+(single?1:2));i++){
            double y=9+(i-lyricRow)*20;String line=i<0?"♪":lines.get(i).text();
            int colour=i==index?0xffeeeeee:0xff999999;
            g.pose().pushMatrix();g.pose().translate(0,(float)y);
            boolean wordTimed=i>=0&&!lines.get(i).words().isEmpty();
            text(g,shorten(line.isBlank()?"♪":line,230),7,0,wordTimed?0xff888888:colour);
            if(wordTimed&&i==index){
                double fill=0;for(var word:lines.get(i).words()){
                    int from=Math.min(line.length(),word.from()),to=Math.min(line.length(),word.to());
                    if(pos<word.start())continue;
                    double progress=word.end()<=word.start()?1:Math.clamp((pos-word.start())/(word.end()-word.start()),0,1);
                    double start=MC.font.width(line.substring(0,from)),end=MC.font.width(line.substring(0,to));fill=Math.max(fill,start+(end-start)*progress);
                }
                if(fill>0){g.enableScissor(7,-1,7+(int)Math.ceil(Math.min(230,fill)),10);text(g,shorten(line,230),7,0,0xffffffff);g.disableScissor();}
            }
            g.pose().popMatrix();
        }
        g.disableScissor();
    }
    public static boolean openLyricCredit(double mx,double my){
        var r=bounds.get(Module.LYRICS);var base=baseBounds.get(Module.LYRICS);if(r==null||base==null||!r.contains(mx,my))return false;
        double scale=(double)r.w()/base.w();double localY=(my-r.y())/scale;int bodyH=Aeternum.config.lyricsSingleLine?26:46;
        int index=(int)Math.floor((localY-bodyH+2)/11);var rows=LyricsCredits.rows(Aeternum.media.lyrics.result,230);
        if(index>=0&&index<rows.size()&&!rows.get(index).url().isBlank()){LyricsCredits.open(rows.get(index).url());return true;}return false;
    }
    private static String time(double value){int s=(int)Math.max(0,value);return String.format(Locale.ROOT,"%d:%02d",s/60,s%60);}    public static void line(GuiGraphics g,int x,int y,int tx,int ty,int c){int n=Math.max(Math.abs(tx-x),Math.abs(ty-y));for(int i=0;i<=n;i++){double t=n==0?0:(double)i/n;int px=(int)Math.round(x+(tx-x)*t),py=(int)Math.round(y+(ty-y)*t);g.fill(px,py,px+1,py+1,c);}}
}





