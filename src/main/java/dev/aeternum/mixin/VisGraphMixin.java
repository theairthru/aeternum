package dev.aeternum.mixin;
import dev.aeternum.Xray;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(VisGraph.class)
public class VisGraphMixin {
    @Inject(method="setOpaque",at=@At("HEAD"),cancellable=true)
    private void aeternum$visibility(BlockPos pos,CallbackInfo ci){if(Xray.active())ci.cancel();}
}
