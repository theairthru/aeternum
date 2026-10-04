package dev.aeternum.mixin;
import dev.aeternum.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=FluidRendering.class,remap=false)
public class FluidRenderingMixin {
    private static final FluidRendering.DefaultRenderer AETERNUM_VANILLA_FLUID=new FluidRendering.DefaultRenderer(){};
    @ModifyVariable(method="render",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static FluidRendering.DefaultRenderer aeternum$default(FluidRendering.DefaultRenderer original){return Xray.active()?AETERNUM_VANILLA_FLUID:original;}
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private static void aeternum$skip(FluidRenderHandler h,BlockAndTintGetter view,BlockPos pos,VertexConsumer v,BlockState state,FluidState fluid,FluidRendering.DefaultRenderer renderer,CallbackInfo ci){if(Xray.hidden(fluid.createLegacyBlock())&&Aeternum.config.xrayOpacity<=0)ci.cancel();}
    @ModifyVariable(method="render",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static VertexConsumer aeternum$alpha(VertexConsumer original,FluidRenderHandler h,BlockAndTintGetter view,BlockPos pos,VertexConsumer v,BlockState state,FluidState fluid,FluidRendering.DefaultRenderer renderer){return Xray.hidden(fluid.createLegacyBlock())?new AlphaConsumer(original,(float)Aeternum.config.xrayOpacity):original;}
}
