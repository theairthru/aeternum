package dev.aeternum;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class ClientScreen extends Screen {
    private String query="",category="All";
    private double scroll;
    private int left,top,panelW,panelH,viewTop,viewBottom,cols,size,maxScroll;
    private java.util.List<Module> visible=java.util.List.of();
    private static final String[] CATEGORIES={"All","HUD","Combat","Camera","World","Movement","Cosmetic","System"};
    public ClientScreen(){super(Component.literal("Aeternum"));}
    @Override protected void init(){
        clearWidgets();panelW=Math.min(600,width-28);panelH=Math.min(440,height-28);left=(width-panelW)/2;top=(height-panelH)/2;
        var search=new EditBox(font,left+16,top+43,Math.max(60,panelW-254),20,Component.literal("Search modules"));search.setHint(Component.literal("Search modules…"));search.setValue(query);
        search.setResponder(s->{query=s;scroll=0;filter();});addRenderableWidget(search);
        addRenderableWidget(new UiButton(left+panelW-230,top+43,100,20,category,b->{category=CATEGORIES[(Arrays.asList(CATEGORIES).indexOf(category)+1)%CATEGORIES.length];scroll=0;rebuildWidgets();}));
        addRenderableWidget(new UiButton(left+panelW-122,top+43,106,20,"Edit overlays",b->minecraft.setScreen(new HudEditor(this))));
        addRenderableWidget(new UiButton(left+panelW-72,top+panelH-29,56,20,"Close",b->onClose()));
        viewTop=top+76;viewBottom=top+panelH-40;cols=Math.max(2,(panelW-32)/116);size=(panelW-32-(cols-1)*10)/cols;filter();
    }
    private void filter(){visible=Arrays.stream(Module.values()).filter(m->(category.equals("All")||m.category.equals(category))&&(m.title+" "+m.description).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();maxScroll=Math.max(0,((visible.size()+cols-1)/cols)*(size+10)-10-(viewBottom-viewTop));scroll=Math.clamp(scroll,0,maxScroll);}
    public static void backdrop(GuiGraphics g,int w,int h){int pw=Math.min(550,w-16);Shapes.round(g,(w-pw)/2,6,pw,h-12,14,0xc0181818);}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        Shapes.round(g,left,top,panelW,panelH,16,0xc4181818);
        g.drawString(font,"A E T E R N U M",left+17,top+18,0xffeeeeee,false);
        g.drawString(font,"CLIENT",left+panelW-54,top+19,0xff999999,false);
        g.enableScissor(left+12,viewTop,left+panelW-12,viewBottom);
        for(int i=0;i<visible.size();i++){
            Module m=visible.get(i);int x=left+16+i%cols*(size+10),y=viewTop+i/cols*(size+10)-(int)scroll;
            if(y+size<viewTop||y>viewBottom)continue;
            boolean hover=mx>=x&&mx<x+size&&my>=Math.max(y,viewTop)&&my<Math.min(y+size,viewBottom);
            Shapes.round(g,x,y,size,size,11,hover?0x904c4c4c:0x70373737);
            g.drawString(font,m.category.toUpperCase(Locale.ROOT),x+10,y+12,0xffa0a0a0,false);
            var lines=font.split(Component.literal(m.title),size-20);int ly=y+size/2-10;
            for(var line:lines){g.drawString(font,line,x+10,ly,0xfff0f0f0,false);ly+=11;}
            Shapes.round(g,x+10,y+size-28,size-20,19,7,m.enabled()?0xc0606060:0x80404040);
            g.drawCenteredString(font,m.enabled()?"ON":"OFF",x+size/2,y+size-22,0xffffffff);
        }
        g.disableScissor();
        if(maxScroll>0){int h=Math.max(18,(viewBottom-viewTop)*(viewBottom-viewTop)/(maxScroll+viewBottom-viewTop));Shapes.round(g,left+panelW-7,viewTop+(int)((viewBottom-viewTop-h)*scroll/maxScroll),3,h,1,0x90999999);}
        g.drawString(font,"Scroll to browse  ·  Click a tile for settings",left+16,top+panelH-22,0xffaaaaaa,false);
        super.render(g,mx,my,delta);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(x>=left&&x<left+panelW&&y>=viewTop&&y<viewBottom){scroll=Math.clamp(scroll-vertical*35,0,maxScroll);return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(super.mouseClicked(e,twice))return true;
        if(e.button()!=0||e.y()<viewTop||e.y()>=viewBottom)return false;
        for(int i=0;i<visible.size();i++){int x=left+16+i%cols*(size+10),y=viewTop+i/cols*(size+10)-(int)scroll;if(e.x()>=x&&e.x()<x+size&&e.y()>=y&&e.y()<y+size){Module m=visible.get(i);if(e.y()>=y+size-28&&e.y()<y+size-9&&e.x()>=x+10&&e.x()<x+size-10)m.toggle();else minecraft.setScreen(new SettingsScreen(this,m));return true;}}
        return false;
    }
    @Override public boolean keyPressed(KeyEvent e){if(Aeternum.menu.matches(e)){onClose();return true;}return super.keyPressed(e);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){Aeternum.config.save();minecraft.setScreen(null);}
}


