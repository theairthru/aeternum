package dev.aeternum.mixin;
import dev.aeternum.Aeternum;
import dev.aeternum.CapeStore;
import dev.aeternum.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class CapeMixin {
    @Inject(method="getSkin",at=@At("RETURN"),cancellable=true)
    private void aeternum$cape(CallbackInfoReturnable<PlayerSkin> ci){
        if((Object)this!=Minecraft.getInstance().player||Aeternum.config==null||!Module.CAPE.enabled()||!CapeStore.available())return;
        PlayerSkin skin=ci.getReturnValue();
        ci.setReturnValue(new PlayerSkin(skin.body(),new ClientAsset.ResourceTexture(Aeternum.id("cape"),CapeStore.TEXTURE),skin.elytra(),skin.model(),skin.secure()));
    }
}
