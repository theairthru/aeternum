package dev.aeternum;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditor extends Screen {
    private final Screen parent;
    private Module dragging;
    private double offsetX,offsetY;
    private boolean resizing;
    private Hud.Rect resizeStart,resizeBase;
    private int guideX=-1,guideY=-1;
    public HudEditor(Screen parent){super(Component.literal("Edit overlays"));this.parent=parent;}
    @Override protected void init(){
        addRenderableWidget(new UiButton(width/2-114,8,142,20,"Snapping: "+(Aeternum.config.snapping?"ON":"OFF"),b->{Aeternum.config.snapping=!Aeternum.config.snapping;rebuildWidgets();}));
        addRenderableWidget(new UiButton(width/2+34,8,80,20,"Done",b->onClose()));
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float d){
        g.fill(0,0,width,height,0x26000000);Hud.render(g,true);
        for(var entry:Hud.bounds.entrySet()){
            Hud.Rect r=entry.getValue();int c=entry.getKey()==dragging?0xffeeeeee:0x90bbbbbb;
            g.fill(r.x(),r.y(),r.x()+r.w(),r.y()+1,c);g.fill(r.x(),r.y()+r.h()-1,r.x()+r.w(),r.y()+r.h(),c);
            g.fill(r.x(),r.y(),r.x()+1,r.y()+r.h(),c);g.fill(r.x()+r.w()-1,r.y(),r.x()+r.w(),r.y()+r.h(),c);
            Shapes.round(g,r.x()+r.w()-7,r.y()+r.h()-7,7,7,2,0xffeeeeee);
            if(r.contains(mx,my))g.drawString(font,entry.getKey().title,r.x(),Math.max(30,r.y()-11),0xffeeeeee,true);
        }
        if(guideX>=0)g.fill(guideX,30,guideX+1,height,0x90bbbbbb);
        if(guideY>=0)g.fill(0,guideY,width,guideY+1,0x90bbbbbb);
        super.render(g,mx,my,d);
        g.drawCenteredString(font,"Drag to move • drag corner to resize • right-click for settings",width/2,36,0xffdddddd);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(e.button()!=1&&super.mouseClicked(e,twice))return true;
        if(e.button()==0&&Hud.openLyricCredit(e.x(),e.y()))return true;
        var modules=new java.util.ArrayList<>(Hud.bounds.keySet());java.util.Collections.reverse(modules);
        if(e.button()==0||e.button()==1)for(Module module:modules){Hud.Rect r=Hud.bounds.get(module);if(r.contains(e.x(),e.y())){
            if(e.button()==1){minecraft.setScreen(new SettingsScreen(this,module));return true;}
            dragging=module;resizing=e.x()>=r.x()+r.w()-9&&e.y()>=r.y()+r.h()-9;
            resizeStart=r;resizeBase=Hud.baseBounds.get(module);offsetX=e.x()-r.x();offsetY=e.y()-r.y();return true;
        }}
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(dragging==null)return super.mouseDragged(e,dx,dy);
        if(resizing&&resizeBase!=null){
            double bw=resizeBase.w(),bh=resizeBase.h();
            double scale=((e.x()-resizeStart.x())*bw+(e.y()-resizeStart.y())*bh)/(bw*bw+bh*bh);
            dragging.state().scale=Math.clamp(scale,.5,3);
            double effective=Math.min(dragging.state().scale,Math.min(width/bw,height/bh));
            int rw=(int)Math.ceil(bw*effective),rh=(int)Math.ceil(bh*effective);
            dragging.state().x=Math.clamp((double)resizeStart.x()/Math.max(1,width-rw),0,1);
            dragging.state().y=Math.clamp((double)resizeStart.y()/Math.max(1,height-rh),0,1);
            return true;
        }
        Hud.Rect r=Hud.bounds.get(dragging);if(r==null)return false;
        double x=e.x()-offsetX,y=e.y()-offsetY;guideX=guideY=-1;
        if(Aeternum.config.snapping){
            double[] sx={0,(width-r.w())/2d,width-r.w()},sy={0,(height-r.h())/2d,height-r.h()};
            for(double s:sx)if(Math.abs(x-s)<6){x=s;guideX=(int)(s==sx[1]?width/2:s);break;}
            for(double s:sy)if(Math.abs(y-s)<6){y=s;guideY=(int)(s==sy[1]?height/2:s);break;}
            for(var entry:Hud.bounds.entrySet())if(entry.getKey()!=dragging){Hud.Rect b=entry.getValue();
                double[] tx={b.x(),b.x()+b.w()-r.w(),b.x()+(b.w()-r.w())/2d,b.x()+b.w()+4,b.x()-r.w()-4};
                double[] ty={b.y(),b.y()+b.h()-r.h(),b.y()+(b.h()-r.h())/2d,b.y()+b.h()+4,b.y()-r.h()-4};
                for(double t:tx)if(Math.abs(x-t)<5){x=t;guideX=(int)t;break;}
                for(double t:ty)if(Math.abs(y-t)<5){y=t;guideY=(int)t;break;}
            }
        }
        dragging.state().x=Math.clamp(x/Math.max(1,width-r.w()),0,1);dragging.state().y=Math.clamp(y/Math.max(1,height-r.h()),0,1);return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){dragging=null;resizing=false;guideX=guideY=-1;Aeternum.config.save();return super.mouseReleased(e);}
    @Override public void removed(){dragging=null;resizing=false;Aeternum.config.save();}
    @Override public void onClose(){Aeternum.config.save();minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

