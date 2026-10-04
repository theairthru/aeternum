package dev.aeternum.mixin;
import dev.aeternum.Aeternum;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void aeternum$attack(CallbackInfoReturnable<Boolean> ci){if(Aeternum.camera.freecamActive)ci.setReturnValue(false);}
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void aeternum$use(CallbackInfo ci){if(Aeternum.camera.freecamActive)ci.cancel();}
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void aeternum$mine(boolean attack,CallbackInfo ci){if(Aeternum.camera.freecamActive)ci.cancel();}
}
