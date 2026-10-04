package dev.aeternum;

import java.awt.image.BufferedImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Retains the decoded original until the user applies a crop or cancels. */
public final class CapeImportScreen extends Screen {
    private static final Identifier PREVIEW=Aeternum.id("cape-import-preview");
    private final CapeScreen parent;
    private final BufferedImage source;
    private boolean crop=true,dragging,uploaded;
    private double zoom=1,cx=.5,cy=.5;
    private int x,y,w,h,left,top,panelW,panelH;
    private String message="Drag to position; scroll to zoom. Original detail is preserved.";
    public CapeImportScreen(CapeScreen parent,BufferedImage source){super(Component.literal("Crop cape image"));this.parent=parent;this.source=source;}
    @Override protected void init(){
        panelW=Math.min(600,width-20);panelH=Math.min(410,height-20);left=(width-panelW)/2;top=(height-panelH)/2;
        h=Math.max(40,panelH-112);w=h*10/16;x=left+25;y=top+40;
        int bx=left+panelW/2,bw=panelW/2-20;
        addRenderableWidget(new UiButton(bx,top+45,bw,22,crop?"Mode: crop to fill":"Mode: fit whole image",b->{crop=!crop;zoom=1;cx=cy=.5;rebuildWidgets();}));
        addRenderableWidget(new UiButton(bx,top+76,bw/2-3,22,"Zoom -",b->{crop=true;zoom=Math.max(1,zoom/1.2);}));
        addRenderableWidget(new UiButton(bx+bw/2+3,top+76,bw/2-3,22,"Zoom +",b->{crop=true;zoom=Math.min(16,zoom*1.2);}));
        addRenderableWidget(new UiButton(bx,top+107,bw,22,"Reset position",b->{zoom=1;cx=cy=.5;}));
        if(CapeStore.isAtlas(source))addRenderableWidget(new UiButton(bx,top+138,bw,22,"Use original cape atlas",b->useAtlas()));
        addRenderableWidget(new UiButton(left+16,top+panelH-28,85,20,"Cancel",b->onClose()));
        addRenderableWidget(new UiButton(left+panelW-116,top+panelH-28,100,20,"Apply image",b->apply()));
    }
    double[] selection(){
        double sw=source.getWidth(),sh=source.getHeight();if(!crop)return new double[]{0,0,sw,sh};
        double ch=Math.min(sh,sw*1.6)/zoom,cw=ch/1.6;
        double sx=Math.clamp(cx*sw-cw/2,0,sw-cw),sy=Math.clamp(cy*sh-ch/2,0,sh-ch);
        return new double[]{sx,sy,cw,ch};
    }
    public void useAtlas(){parent.acceptImport(CapeStore.copy(source));minecraft.setScreen(parent);}
    public void apply(){try{var s=selection();parent.acceptImport(CapeStore.fit(source,s[0],s[1],s[2],s[3]));minecraft.setScreen(parent);}catch(Exception e){message=e.getMessage();}}
    public Hud.Rect cropBounds(){return new Hud.Rect(x,y,w,h);}
    private boolean inside(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(e.button()==0&&inside(e.x(),e.y())&&crop){dragging=true;return true;}return super.mouseClicked(e,twice);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(dragging){var s=selection();cx=Math.clamp((s[0]+s[2]/2-dx*s[2]/w)/source.getWidth(),0,1);cy=Math.clamp((s[1]+s[3]/2-dy*s[3]/h)/source.getHeight(),0,1);return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){dragging=false;return super.mouseReleased(e);}
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){if(inside(mx,my)){crop=true;zoom=Math.clamp(zoom*Math.pow(1.15,dy),1,16);return true;}return super.mouseScrolled(mx,my,dx,dy);}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float d){
        Shapes.round(g,left,top,panelW,panelH,12,0xe0181818);g.drawString(font,"CROP / FIT CAPE",left+16,top+15,0xffeeeeee,false);
        if(!uploaded){try{CapeStore.upload(PREVIEW,source);uploaded=true;}catch(Exception e){message="Preview unavailable";}}
        for(int yy=0;yy<h;yy+=12)for(int xx=0;xx<w;xx+=12)g.fill(x+xx,y+yy,x+Math.min(w,xx+12),y+Math.min(h,yy+12),((xx/12+yy/12)&1)==0?0xff454545:0xff303030);
        var s=selection();int dw=w,dh=h,px=x,py=y;
        if(!crop){double scale=Math.min((double)w/source.getWidth(),(double)h/source.getHeight());dw=(int)Math.round(source.getWidth()*scale);dh=(int)Math.round(source.getHeight()*scale);px+=(w-dw)/2;py+=(h-dh)/2;}
        if(uploaded)g.blit(PREVIEW,px,py,px+dw,py+dh,(float)(s[0]/source.getWidth()),(float)((s[0]+s[2])/source.getWidth()),(float)(s[1]/source.getHeight()),(float)((s[1]+s[3])/source.getHeight()));
        int bx=left+panelW/2;g.drawString(font,"Original: "+source.getWidth()+" x "+source.getHeight(),bx,top+180,0xffbbbbbb,false);
        g.drawString(font,"Selection: "+(int)s[2]+" x "+(int)s[3],bx,top+196,0xffbbbbbb,false);
        g.drawString(font,"Zoom: "+String.format(java.util.Locale.ROOT,"%.2fx",zoom),bx,top+212,0xffbbbbbb,false);
        g.drawString(font,font.plainSubstrByWidth(message,panelW-32),left+16,top+panelH-47,0xffcccccc,false);super.render(g,mx,my,d);
    }
    @Override public void removed(){minecraft.getTextureManager().release(PREVIEW);uploaded=false;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
