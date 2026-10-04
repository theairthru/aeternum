package dev.aeternum.mixin;

import dev.aeternum.Aeternum;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method="turn",at=@At("HEAD"),cancellable=true)
    private void aeternum$turn(double x,double y,CallbackInfo ci){if((Object)this==Minecraft.getInstance().player&&Aeternum.camera.turn(x,y))ci.cancel();}
}
