package dev.aeternum.mixin;
import dev.aeternum.Hud;
import net.minecraft.client.*;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void aeternum$scroll(long window,double horizontal,double vertical,CallbackInfo ci){
        if(dev.aeternum.Aeternum.zooming()){
            var c=dev.aeternum.Aeternum.config;c.zoomFactor=Math.clamp(c.zoomFactor*Math.pow(1.15,vertical),1.5,12);c.save();ci.cancel();
        }
    }
    @Inject(method="onButton",at=@At("HEAD"))
    private void aeternum$click(long window,MouseButtonInfo info,int action,CallbackInfo ci){if(action==1&&Minecraft.getInstance().screen==null)Hud.click(info.button());}
}
