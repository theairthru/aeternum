package dev.aeternum;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class BlockScreen extends Screen {
    private final Screen parent;
    private String search="";
    private int page;
    public BlockScreen(Screen parent){super(Component.literal("Visible blocks"));this.parent=parent;}
    @Override protected void init(){
        clearWidgets();int w=Math.min(500,width-32),x=(width-w)/2;
        EditBox field=new EditBox(font,x,40,w,20,Component.literal("Search blocks"));field.setHint(Component.literal("Search all registered blocks…"));field.setValue(search);field.setResponder(s->{if(!search.equals(s)){search=s;page=0;rebuildWidgets();}});addRenderableWidget(field);setInitialFocus(field);field.moveCursorToEnd(false);
        var list=BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString).filter(s->s.contains(search.toLowerCase(Locale.ROOT))).sorted().toList();
        int per=Math.max(1,(height-100)/24),pages=Math.max(1,(list.size()+per-1)/per);page=Math.clamp(page,0,pages-1);
        for(int i=page*per;i<Math.min(list.size(),(page+1)*per);i++){
            String id=list.get(i);addRenderableWidget(new UiButton(x,68+(i-page*per)*24,w,21,(Aeternum.config.xrayBlocks.contains(id)?"[x] ":"[ ] ")+id,b->{if(!Aeternum.config.xrayBlocks.remove(id))Aeternum.config.xrayBlocks.add(id);rebuildWidgets();}));
        }
        addRenderableWidget(new UiButton(x,height-26,50,20,"<",b->{page=(page+pages-1)%pages;rebuildWidgets();}));
        addRenderableWidget(new UiButton(x+56,height-26,100,20,(page+1)+" / "+pages,b->{page=(page+1)%pages;rebuildWidgets();}));
        addRenderableWidget(new UiButton(x+w-90,height-26,90,20,"Back",b->onClose()));
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){}
    @Override public void render(GuiGraphics g,int x,int y,float d){ClientScreen.backdrop(g,width,height);g.drawString(font,"X-RAY / VISIBLE BLOCKS",(width-Math.min(500,width-32))/2,19,0xffeeeeee,false);super.render(g,x,y,d);}
    @Override public void onClose(){Aeternum.config.save();Xray.refresh();minecraft.levelRenderer.allChanged();minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

