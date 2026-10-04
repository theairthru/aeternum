package dev.aeternum.mixin;
import dev.aeternum.Culling;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderDispatcher.class)
public class EntityRendererMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private void aeternum$cull(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){if(Culling.hidden(entity))ci.setReturnValue(false);}
}
