package dev.aeternum.mixin;
import dev.aeternum.Aeternum;
import net.minecraft.client.player.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method="tick",at=@At("TAIL"))
    private void aeternum$input(CallbackInfo ci){if(Aeternum.camera.freecamActive){keyPresses=new Input(false,false,false,false,false,false,false);moveVector=Vec2.ZERO;}}
}
