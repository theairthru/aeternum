package dev.aeternum.mixin;
import dev.aeternum.Xray;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemBlockRenderTypes.class)
public class FluidLayerMixin {
    @Inject(method="getRenderLayer",at=@At("HEAD"),cancellable=true)
    private static void aeternum$layer(FluidState state,CallbackInfoReturnable<ChunkSectionLayer> ci){if(Xray.hidden(state.createLegacyBlock()))ci.setReturnValue(ChunkSectionLayer.TRANSLUCENT);}
}
