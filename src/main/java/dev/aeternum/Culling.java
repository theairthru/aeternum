package dev.aeternum;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

public final class Culling {
    private record Result(long tick,boolean hidden){}
    private static final Map<Integer,Result> cache=new HashMap<>();
    private static long tick;
    private static int budget;
    private static Object world;
    public static void tick(){tick++;budget=12;Object current=Minecraft.getInstance().level;if(current!=world){cache.clear();world=current;}if(tick%100==0)cache.entrySet().removeIf(e->tick-e.getValue().tick>40);}
    public static boolean hidden(Entity entity){
        Minecraft mc=Minecraft.getInstance();
        if(Aeternum.config==null||!Module.PERFORMANCE.enabled()||mc.level==null||entity==mc.player||entity.isCurrentlyGlowing())return false;
        if(entity instanceof Player&&!Aeternum.config.cullPlayers)return false;
        Vec3 eye=mc.gameRenderer.getMainCamera().position();
        double dist=entity.position().distanceToSqr(eye),range=Aeternum.config.entityDistance;
        if(dist>range*range)return true;
        if(!Aeternum.config.occlusionCulling||dist<64||entity.getBbWidth()>4||entity.getBbHeight()>4)return false;
        Result result=cache.get(entity.getId());if(result!=null&&tick-result.tick<4)return result.hidden;
        if(budget<=0)return false;budget--;
        AABB b=entity.getBoundingBox().inflate(.15);
        // Never hide a box with any visible sampled corner. Near/large entities stay visible.
        for(int i=0;i<9;i++){
            Vec3 target=i==8?b.getCenter():new Vec3((i&1)==0?b.minX:b.maxX,(i&2)==0?b.minY:b.maxY,(i&4)==0?b.minZ:b.maxZ);
            if(mc.level.clip(new ClipContext(eye,target,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS){cache.put(entity.getId(),new Result(tick,false));return false;}
        }
        cache.put(entity.getId(),new Result(tick,true));return true;
    }
}
