package dev.aeternum;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class CapeScreen extends Screen {
    private static final Identifier PREVIEW=Aeternum.id("cape-paint-preview");
    private final Screen parent;
    private BufferedImage image=CapeStore.editable();
    private final ArrayDeque<BufferedImage> undo=new ArrayDeque<>();
    private String hex="EEEEEE",message="Paint a face, or import / drop PNG, JPEG or WebP. Nothing is uploaded.";
    private boolean outside=true,eraser,dirty=true,painting,dialog;
    private int color=0xffeeeeee,brush=1,lastX=-1,lastY=-1;
    private int left,top,panelW,panelH,canvasX,canvasY,canvasW,canvasH,sideX,sideW;
    public CapeScreen(Screen parent){super(Component.literal("Cape painter"));this.parent=parent;}
    @Override protected void init(){
        clearWidgets();panelW=Math.min(620,width-20);panelH=Math.min(440,height-20);left=(width-panelW)/2;top=(height-panelH)/2;
        int availableW=Math.max(50,panelW/2-30),availableH=Math.max(50,panelH-108);
        canvasH=Math.min(availableH,availableW*16/10);canvasW=canvasH*10/16;canvasX=left+20+(availableW-canvasW)/2;canvasY=top+47;
        sideX=left+panelW/2;sideW=panelW/2-18;
        addRenderableWidget(new UiButton(sideX,top+44,sideW,21,outside?"Face: outside / back":"Face: inside / front",b->{outside=!outside;rebuildWidgets();}));
        EditBox colour=new EditBox(font,sideX,top+82,sideW-38,20,Component.literal("Hex colour"));colour.setMaxLength(7);colour.setValue(hex);
        colour.setResponder(s->{hex=s;try{if(!s.matches("#?[0-9a-fA-F]{6}"))throw new IllegalArgumentException();color=0xff000000|Integer.parseInt(s.replace("#",""),16);colour.setTextColor(0xffeeeeee);}catch(Exception e){colour.setTextColor(0xffff7777);}});addRenderableWidget(colour);
        addRenderableWidget(new UiButton(sideX,top+110,sideW/2-3,21,"Brush: "+brush+" px",b->{brush=brush==1?2:brush==2?4:1;rebuildWidgets();}));
        addRenderableWidget(new UiButton(sideX+sideW/2+3,top+110,sideW/2-3,21,eraser?"Eraser":"Paint",b->{eraser=!eraser;rebuildWidgets();}));
        addRenderableWidget(new UiButton(sideX,top+139,sideW/2-3,21,"Undo",b->undo()));
        addRenderableWidget(new UiButton(sideX+sideW/2+3,top+139,sideW/2-3,21,"Fill face",b->{snapshot();for(int y=0;y<faceH();y++)for(int x=0;x<faceW();x++)put(x,y,eraser?0:color);dirty=true;}));
        addRenderableWidget(new UiButton(sideX,top+168,sideW,21,"Import image…",b->choose(false)));
        addRenderableWidget(new UiButton(sideX,top+197,sideW,21,"Export PNG…",b->choose(true)));
        addRenderableWidget(new UiButton(left+16,top+panelH-29,90,20,"Cancel",b->onClose()));
        addRenderableWidget(new UiButton(left+panelW-136,top+panelH-29,120,20,"Save & wear",b->saveAndWear()));
    }
    private int unit(){return image.getWidth()/64;}
    private int faceW(){return 10*unit();}
    private int faceH(){return 16*unit();}
    private int uvX(){return (outside?1:12)*unit();}
    public Hud.Rect canvasBounds(){return new Hud.Rect(canvasX,canvasY,canvasW,canvasH);}
    private void snapshot(){undo.addLast(CapeStore.copy(image));while(undo.size()>1&&(undo.size()>8||undo.stream().mapToLong(i->(long)i.getWidth()*i.getHeight()).sum()>32_000_000))undo.removeFirst();}
    public void undo(){if(!undo.isEmpty()){image=undo.removeLast();dirty=true;}}
    private void put(int x,int y,int argb){if(x>=0&&x<faceW()&&y>=0&&y<faceH())image.setRGB(uvX()+x,unit()+y,argb);}
    private boolean inCanvas(double x,double y){return x>=canvasX&&x<canvasX+canvasW&&y>=canvasY&&y<canvasY+canvasH;}
    private void paint(double x,double y,boolean erase){
        int px=Math.clamp((int)((x-canvasX)*faceW()/canvasW),0,faceW()-1),py=Math.clamp((int)((y-canvasY)*faceH()/canvasH),0,faceH()-1);
        int sx=lastX<0?px:lastX,sy=lastY<0?py:lastY,steps=Math.max(Math.abs(px-sx),Math.abs(py-sy));
        for(int i=0;i<=steps;i++){double t=steps==0?0:(double)i/steps;int cx=(int)Math.round(sx+(px-sx)*t),cy=(int)Math.round(sy+(py-sy)*t);
            for(int by=0;by<brush;by++)for(int bx=0;bx<brush;bx++)put(cx+bx-brush/2,cy+by-brush/2,erase?0:color);}
        lastX=px;lastY=py;dirty=true;
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(inCanvas(e.x(),e.y())&&(e.button()==0||e.button()==1)){snapshot();painting=true;lastX=lastY=-1;paint(e.x(),e.y(),eraser||e.button()==1);return true;}
        return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(painting){if(inCanvas(e.x(),e.y()))paint(e.x(),e.y(),eraser||e.button()==1);else lastX=lastY=-1;return true;}return super.mouseDragged(e,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent e){painting=false;lastX=lastY=-1;return super.mouseReleased(e);}
    public void importPath(Path path){try{BufferedImage source=CapeStore.readSource(path);minecraft.setScreen(new CapeImportScreen(this,source));}catch(Exception e){message=e.getMessage()==null?"Could not import image.":e.getMessage();}}
    void acceptImport(BufferedImage next){snapshot();image=next;dirty=true;message="Image imported at "+next.getWidth()+" x "+next.getHeight()+". Save & wear to apply.";}
    @Override public void onFilesDrop(java.util.List<Path> files){if(!files.isEmpty())importPath(files.getFirst());}
    private void choose(boolean export){
        if(dialog)return;dialog=true;message=export?"Choose a PNG destination…":"Choose a PNG, JPEG or WebP image…";
        java.util.concurrent.CompletableFuture.supplyAsync(()->{
            try(MemoryStack stack=MemoryStack.stackPush()){
                var filters=stack.mallocPointer(export?1:4);filters.put(stack.UTF8("*.png"));if(!export){filters.put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg")).put(stack.UTF8("*.webp"));}filters.flip();
                return export?TinyFileDialogs.tinyfd_saveFileDialog("Export cape PNG","aeternum-cape.png",filters,"PNG image"):TinyFileDialogs.tinyfd_openFileDialog("Import cape image","",filters,"PNG, JPEG or WebP image",false);
            }
        }).whenComplete((file,error)->minecraft.execute(()->{dialog=false;if(minecraft.screen!=this)return;
            if(error!=null){message="File picker unavailable. Drop a PNG, JPEG or WebP onto this window to import.";return;}
            if(file==null){message="File selection cancelled.";return;}
            if(export){try{Path path=Path.of(file);javax.imageio.ImageIO.write(image,"png",path.toFile());message="Exported "+path.getFileName();}catch(Exception e){message="Could not export PNG: "+e.getMessage();}}
            else importPath(Path.of(file));
        }));
    }
    public void saveAndWear(){try{CapeStore.save(image);Module.CAPE.state().enabled=true;Aeternum.config.save();minecraft.setScreen(parent);}catch(Exception e){message="Could not save cape: "+e.getMessage();}}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float d){
        Shapes.round(g,left,top,panelW,panelH,14,0xe0181818);g.drawString(font,"CAPE PAINTER",left+17,top+17,0xffeeeeee,false);
        if(dirty){try{CapeStore.upload(PREVIEW,image);dirty=false;}catch(Exception e){message="Preview unavailable: "+e.getMessage();dirty=false;}}
        for(int y=0;y<canvasH;y+=12)for(int x=0;x<canvasW;x+=12)g.fill(canvasX+x,canvasY+y,canvasX+Math.min(canvasW,x+12),canvasY+Math.min(canvasH,y+12),((x/12+y/12)&1)==0?0xff454545:0xff303030);
        float u0=uvX()/(float)image.getWidth(),u1=(uvX()+faceW())/(float)image.getWidth(),v0=unit()/(float)image.getHeight(),v1=(unit()+faceH())/(float)image.getHeight();
        g.blit(PREVIEW,canvasX,canvasY,canvasX+canvasW,canvasY+canvasH,u0,u1,v0,v1);
        if(canvasW/faceW()>=6){for(int i=0;i<=faceW();i++){int x=canvasX+i*canvasW/faceW();g.fill(x,canvasY,x+1,canvasY+canvasH,0x40444444);}for(int i=0;i<=faceH();i++){int y=canvasY+i*canvasH/faceH();g.fill(canvasX,y,canvasX+canvasW,y+1,0x40444444);}}
        g.drawString(font,"Hex colour",sideX,top+70,0xffaaaaaa,false);Shapes.round(g,sideX+sideW-28,top+81,27,22,4,color);
        g.drawString(font,"Right-click canvas to erase",sideX,top+229,0xffaaaaaa,false);
        g.drawString(font,"Only you can see this cape.",sideX,top+244,0xffaaaaaa,false);
        g.drawString(font,font.plainSubstrByWidth(message,panelW-32),left+16,top+panelH-47,0xffcccccc,false);
        super.render(g,mx,my,d);
    }
    @Override public void removed(){minecraft.getTextureManager().release(PREVIEW);dirty=true;}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}

