package dev.aeternum.mixin;
import dev.aeternum.Aeternum;
import dev.aeternum.Module;
import net.minecraft.client.*;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method="renderItemInHand",at=@At("HEAD"),cancellable=true)
    private void aeternum$hand(float delta,boolean sleeping,org.joml.Matrix4f projection,CallbackInfo ci){if(Aeternum.camera.freecamActive)ci.cancel();}
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void aeternum$zoom(Camera camera,float delta,boolean changing,CallbackInfoReturnable<Float> ci){if(Aeternum.config!=null&&Module.ZOOM.enabled()&&Module.ZOOM.key.isDown()&&Minecraft.getInstance().screen==null)ci.setReturnValue(ci.getReturnValue()/(float)Aeternum.config.zoomFactor);}
}
