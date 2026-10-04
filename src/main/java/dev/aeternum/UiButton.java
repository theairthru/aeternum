package dev.aeternum;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class UiButton extends Button {
    public UiButton(int x,int y,int w,int h,String label,OnPress action){super(x,y,w,h,Component.literal(label),action,DEFAULT_NARRATION);}
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta){
        int x=getX(),y=getY();Shapes.round(g,x,y,width,height,7,isHoveredOrFocused()?0xb0505050:0x80303030);
        var font=Minecraft.getInstance().font;String text=font.plainSubstrByWidth(getMessage().getString(),width-12);
        g.drawString(font,text,x+6,y+(height-8)/2,active?(text.equals("ON")?0xffffffff:0xffdddddd):0xff777777,false);
    }
}

