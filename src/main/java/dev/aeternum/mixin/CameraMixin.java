package dev.aeternum.mixin;

import dev.aeternum.Aeternum;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void move(float forward,float vertical,float horizontal);
    @Shadow private boolean detached;
    @Shadow private float getMaxZoom(float distance){throw new AssertionError();}
    @Inject(method="setup",at=@At("TAIL"))
    private void aeternum$camera(Level level,Entity entity,boolean third,boolean reverse,float partial,CallbackInfo ci){
        var c=Aeternum.camera;
        if(c.freecamActive){setRotation(c.yaw,c.pitch);setPosition(c.previous.lerp(c.position,partial));detached=true;}
        else if(c.freelookActive){setRotation(c.yaw,c.pitch);setPosition(entity.getEyePosition(partial));move(-getMaxZoom(4),0,0);detached=true;}
    }
}
