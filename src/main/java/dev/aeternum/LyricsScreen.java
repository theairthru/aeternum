package dev.aeternum;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class LyricsScreen extends Screen {
    private final Screen parent;private double scroll;
    public LyricsScreen(Screen parent){super(Component.literal("Lyrics"));this.parent=parent;}
    @Override protected void init(){addRenderableWidget(new UiButton(width/2-45,height-31,90,20,"Back",b->onClose()));}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice){
        int w=Math.min(470,width-48),x=(width-w)/2;var rows=LyricsCredits.rows(Aeternum.media.lyrics.result,w);int i=(int)((e.y()-48)/11);
        if(e.button()==0&&e.x()>=x&&e.x()<x+w&&e.y()>=48&&i>=0&&i<rows.size()){LyricsCredits.open(rows.get(i).url());return true;}return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll=Math.max(0,scroll-v*28);return true;}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        ClientScreen.backdrop(g,width,height);int w=Math.min(470,width-48),x=(width-w)/2;
        g.drawString(font,"LYRICS",x,18,0xffeeeeee,false);g.drawString(font,font.plainSubstrByWidth(SpotifyBridge.current()!=null?SpotifyBridge.current().title():Aeternum.media.title,w),x,33,0xffaaaaaa,false);
        var result=Aeternum.media.lyrics.result;List<String> rows=result.plain();if(rows.isEmpty())rows=result.timed().stream().map(LyricsController.Line::text).toList();if(rows.isEmpty())rows=List.of(result.notice());
        var credits=LyricsCredits.rows(result,w);for(int i=0;i<credits.size();i++)g.drawString(font,credits.get(i).text(),x,48+i*11,0xffbbbbbb,false);int contentTop=57+credits.size()*11;
        if(!result.credit().isBlank()){rows=new ArrayList<>(rows);rows.add("");rows.add(result.credit());}
        List<net.minecraft.util.FormattedCharSequence> wrapped=new ArrayList<>();for(String line:rows){if(line.isBlank())wrapped.add(Component.literal(" ").getVisualOrderText());else wrapped.addAll(font.split(Component.literal(line),w));}
        scroll=Math.clamp(scroll,0,Math.max(0,wrapped.size()*13-(height-contentTop-46)));g.enableScissor(x,contentTop,x+w,height-44);
        for(int i=0;i<wrapped.size();i++){int y=contentTop+i*13-(int)scroll;if(y>contentTop-14&&y<height-44)g.drawString(font,wrapped.get(i),x,y,0xffdddddd,false);}
        g.disableScissor();super.render(g,mx,my,delta);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
