package dev.aeternum;

import net.minecraft.client.*;
import net.minecraft.world.phys.Vec3;

public final class CameraController {
    public boolean freecamActive, freelookActive;
    public float yaw,pitch;
    public Vec3 position=Vec3.ZERO,previous=Vec3.ZERO;
    private CameraType savedPerspective;
    private Object world;
    public void tick(Minecraft mc) {
        if(world!=mc.level) {reset();world=mc.level;Module.FREECAM.state().enabled=false;}
        if(!mc.player.isAlive()) {reset();Module.FREECAM.state().enabled=false;return;}
        boolean free=Module.FREECAM.enabled();
        boolean look=Module.FREELOOK.enabled()&&Module.FREELOOK.key.isDown()&&mc.screen==null&&!free;
        if(free&&!freecamActive) {
            stopLook(mc);yaw=mc.player.getYRot();pitch=mc.player.getXRot();
            position=mc.player.getEyePosition();previous=position;freecamActive=true;
        }
        if(!free&&freecamActive)freecamActive=false;
        if(look&&!freelookActive) {
            savedPerspective=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            yaw=mc.player.getYRot();pitch=mc.player.getXRot();freelookActive=true;
        }
        if(!look)stopLook(mc);
        previous=position;
        if(freecamActive&&mc.screen==null) {
            double f=(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
            double r=(mc.options.keyRight.isDown()?1:0)-(mc.options.keyLeft.isDown()?1:0);
            double u=(mc.options.keyJump.isDown()?1:0)-(mc.options.keyShift.isDown()?1:0);
            double a=Math.toRadians(yaw),p=Math.toRadians(pitch);
            Vec3 direction=new Vec3(-Math.sin(a)*Math.cos(p)*f+-Math.cos(a)*r,-Math.sin(p)*f+u,Math.cos(a)*Math.cos(p)*f-Math.sin(a)*r);
            if(direction.lengthSqr()>0) position=position.add(direction.normalize().scale(Aeternum.config.freecamSpeed*(mc.options.keySprint.isDown()?3:1)));
        }
    }
    public boolean turn(double dx,double dy) {
        if(!freecamActive&&!freelookActive)return false;
        yaw+=(float)(dx*.15);pitch=Math.clamp(pitch+(float)(dy*.15),-90,90);return true;
    }
    private void stopLook(Minecraft mc) {if(freelookActive&&savedPerspective!=null)mc.options.setCameraType(savedPerspective);freelookActive=false;savedPerspective=null;}
    public void reset() {stopLook(Minecraft.getInstance());freecamActive=false;}
}
