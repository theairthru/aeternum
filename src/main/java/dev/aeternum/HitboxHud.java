package dev.aeternum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Projects the actual collision box through Minecraft's camera projection. */
public final class HitboxHud {
    private static final int[][] EDGES={{0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}};
    public static void render(GuiGraphics g){
        Aeternum.renderedHitboxes=0;
        Minecraft mc=Minecraft.getInstance();if(!Module.HITBOXES.enabled()||mc.level==null)return;
        Vec3 eye=mc.gameRenderer.getMainCamera().position();float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        int sw=mc.getWindow().getGuiScaledWidth(),sh=mc.getWindow().getGuiScaledHeight();
        for(Entity e:mc.level.entitiesForRendering()){
            if(e==mc.player||e.isInvisible()||e.position().distanceToSqr(eye)>16384)continue;
            String category=e instanceof Player?"Players":e instanceof Projectile?"Projectiles":e instanceof ItemEntity?"Items":e instanceof LivingEntity?"Mobs":"Other";
            Config.Box style=Aeternum.config.boxes.get(category);if(style==null||!style.enabled)continue;
            AABB b=e.getBoundingBox().move(e.getPosition(partial).subtract(e.position()));
            if(!Aeternum.config.hitboxesThroughWalls&&!visible(mc,eye,e.getEyePosition(partial))&&!visible(mc,eye,b.getCenter()))continue;
            Vec3[] points=new Vec3[8];
            for(int i=0;i<8;i++)points[i]=mc.gameRenderer.projectPointToScreen(new Vec3((i&1)==0?b.minX:b.maxX,(i&2)==0?b.minY:b.maxY,(i&4)==0?b.minZ:b.maxZ));
            for(int[] edge:EDGES){Vec3 a=points[edge[0]],z=points[edge[1]];
                if(a.z< -1||a.z>=1||z.z< -1||z.z>=1)continue;
                line(g,(a.x+1)*sw/2,(1-a.y)*sh/2,(z.x+1)*sw/2,(1-z.y)*sh/2,sw,sh,0xff000000|style.rgb(),Math.clamp((int)Math.round(style.width),1,6));
            }
            Aeternum.renderedHitboxes++;
        }
    }
    private static boolean visible(Minecraft mc,Vec3 eye,Vec3 target){return mc.level.clip(new ClipContext(eye,target,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS;}
    private static int region(double x,double y,int w,int h){return(x<0?1:x>w?2:0)|(y<0?4:y>h?8:0);}
    private static void line(GuiGraphics g,double x,double y,double tx,double ty,int w,int h,int color,int thickness){
        if(!Double.isFinite(x+y+tx+ty))return;
        // Clip before rasterizing: entities crossing the near plane can project far outside the window.
        for(int k=0;k<8;k++){
            int a=region(x,y,w,h),b=region(tx,ty,w,h);if((a|b)==0)break;if((a&b)!=0)return;
            int out=a!=0?a:b;double nx,ny;
            if((out&8)!=0){ny=h;nx=x+(tx-x)*(h-y)/(ty-y);}else if((out&4)!=0){ny=0;nx=x+(tx-x)*(-y)/(ty-y);}else if((out&2)!=0){nx=w;ny=y+(ty-y)*(w-x)/(tx-x);}else{nx=0;ny=y+(ty-y)*(-x)/(tx-x);}
            if(out==a){x=nx;y=ny;}else{tx=nx;ty=ny;}
        }
        Shapes.stroke(g,x,y,tx,ty,thickness,color);
    }
}

