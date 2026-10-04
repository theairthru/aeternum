package dev.aeternum.mixin;
import dev.aeternum.Aeternum;
import dev.aeternum.Module;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LightTexture.class)
public class LightTextureMixin {
    @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",ordinal=3),index=0)
    private float aeternum$bright(float value){return Aeternum.config!=null&&Module.FULLBRIGHT.enabled()?1f:value;}
}
