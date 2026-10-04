package dev.aeternum;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.function.Predicate;

/** Public Fabric renderer API: compatible with renderers that implement that API. */
public record XrayModel(BlockStateModel delegate) implements BlockStateModel {
    @Override public void collectParts(RandomSource r,List<BlockModelPart> parts){delegate.collectParts(r,parts);}
    @Override public TextureAtlasSprite particleIcon(){return delegate.particleIcon();}
    @Override public TextureAtlasSprite particleSprite(BlockAndTintGetter view,BlockPos pos,BlockState state){return delegate.particleSprite(view,pos,state);}
    @Override public Object createGeometryKey(BlockAndTintGetter view,BlockPos pos,BlockState state,RandomSource random){return Xray.active()?null:delegate.createGeometryKey(view,pos,state,random);}
    @Override public void emitQuads(QuadEmitter emitter,BlockAndTintGetter view,BlockPos pos,BlockState state,RandomSource random,Predicate<Direction> cull){
        if(!Xray.active()){delegate.emitQuads(emitter,view,pos,state,random,cull);return;}
        boolean target=Xray.selected(state);float alpha=(float)Aeternum.config.xrayOpacity;
        if(!target&&alpha<=0)return;
        emitter.pushTransform(quad->{
            if(target){quad.cullFace(null);quad.emissive(true);quad.ambientOcclusion(TriState.FALSE);}
            else {quad.renderLayer(ChunkSectionLayer.TRANSLUCENT);for(int i=0;i<4;i++)quad.color(i,((int)(((quad.color(i)>>>24)&255)*alpha)<<24)|(quad.color(i)&0xffffff));}
            return true;
        });
        try{delegate.emitQuads(emitter,view,pos,state,random,target?d->false:cull);}finally{emitter.popTransform();}
    }
}
