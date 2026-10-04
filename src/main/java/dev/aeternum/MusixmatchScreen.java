package dev.aeternum;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class MusixmatchScreen extends Screen {
    private final Screen parent;private EditBox input;
    public MusixmatchScreen(Screen parent){super(Component.literal("Musixmatch API key"));this.parent=parent;}
    @Override protected void init(){
        int x=width/2-150,y=height/2-30;
        input=new EditBox(font,x,y,300,22,Component.literal("API key"));input.setMaxLength(256);input.addFormatter((value,start)->Component.literal("*".repeat(value.length())).getVisualOrderText());addRenderableWidget(input);setInitialFocus(input);
        addRenderableWidget(new UiButton(x,y+38,95,22,"Save key",b->{if(!input.getValue().isBlank()){Aeternum.config.musixmatchApiKey=input.getValue().strip();Aeternum.config.save();Aeternum.media.lyrics.refresh();onClose();}}));
        addRenderableWidget(new UiButton(x+102,y+38,95,22,"Remove key",b->{Aeternum.config.musixmatchApiKey="";Aeternum.config.save();Aeternum.media.lyrics.refresh();onClose();}));
        addRenderableWidget(new UiButton(x+204,y+38,96,22,"Cancel",b->onClose()));
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float d){ClientScreen.backdrop(g,width,height);int x=width/2-150,y=height/2-30;g.drawString(font,"MUSIXMATCH API KEY",x,y-43,0xffeeeeee,false);g.drawString(font,"Stored locally in aeternum.json (not encrypted).",x,y-24,0xffbbbbbb,false);g.drawString(font,"Existing key: "+(Aeternum.config.musixmatchApiKey.isBlank()?"not set":"saved"),x,y+25,0xffbbbbbb,false);super.render(g,mx,my,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
