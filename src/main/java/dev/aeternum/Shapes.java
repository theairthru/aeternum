package dev.aeternum;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Cached antialiased masks: fixed draw count, with no per-pixel work on the render thread. */
public final class Shapes {
    private static final Identifier ROUND=Aeternum.id("textures/gui/rounded.png");
    private static final Identifier CAPSULE=Aeternum.id("textures/gui/capsule.png");
    public static final Identifier DIAL=Aeternum.id("textures/gui/clock.png");
    public static int submittedQuads;
    private Shapes() {}
    private static void piece(GuiGraphics g,Identifier texture,int x,int y,int w,int h,int u,int v,int sw,int sh,int tw,int th,int color){
        if(w<=0||h<=0)return;
        g.blit(RenderPipelines.GUI_TEXTURED,texture,x,y,u,v,w,h,sw,sh,tw,th,color);submittedQuads++;
    }
    public static void round(GuiGraphics g,int x,int y,int w,int h,int radius,int color){
        if(w<=0||h<=0)return;
        int r=Math.min(radius,Math.min(w,h)/2);
        if(r<=0){g.fill(x,y,x+w,y+h,color);submittedQuads++;return;}
        // Nine-slice keeps the corner radius independent of the panel's dimensions.
        int[] dx={x,x+r,x+w-r},dy={y,y+r,y+h-r};
        int[] dw={r,w-2*r,r},dh={r,h-2*r,r};
        int[] source={0,64,192},extent={64,128,64};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)
            piece(g,ROUND,dx[col],dy[row],dw[col],dh[row],source[col],source[row],extent[col],extent[row],256,256,color);
    }
    public static void stroke(GuiGraphics g,double ax,double ay,double bx,double by,double width,int color){
        if(width<=0||!Double.isFinite(ax+ay+bx+by+width))return;
        double length=Math.hypot(bx-ax,by-ay);
        int middle=(int)Math.round(length*32/width);
        g.pose().pushMatrix();
        g.pose().translate((float)ax,(float)ay).rotate((float)Math.atan2(by-ay,bx-ax)).scale((float)(width/32),(float)(width/32)).translate(-16,-16);
        piece(g,CAPSULE,0,0,16,32,0,0,16,32,128,32,color);
        piece(g,CAPSULE,16,0,middle,32,16,0,96,32,128,32,color);
        piece(g,CAPSULE,16+middle,0,16,32,112,0,16,32,128,32,color);
        g.pose().popMatrix();
    }
}
