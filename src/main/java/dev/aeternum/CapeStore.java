package dev.aeternum;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.NativeImage;

/** Local cape storage. No uploads, accounts, packets or remote URLs. */
public final class CapeStore {
    public static final Identifier TEXTURE=Aeternum.id("custom-cape");
    public static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("aeternum/cape.png");
    private static boolean loaded,available;
    private static BufferedImage saved;
    static { javax.imageio.spi.IIORegistry.getDefaultInstance().registerServiceProvider(new com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi()); }
    private CapeStore(){}
    public static boolean available(){
        if(!loaded){loaded=true;try{if(Files.isRegularFile(FILE)){saved=importImage(FILE);upload(TEXTURE,saved);available=true;}}catch(Exception e){Aeternum.LOG.warn("Could not load local cape",e);}}
        return available;
    }
    public static BufferedImage editable(){available();return saved==null?blank():copy(saved);}
    public static BufferedImage blank(){
        var image=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<17;y++)for(int x=0;x<22;x++)image.setRGB(x,y,0xff323232);
        return image;
    }
    public static BufferedImage copy(BufferedImage source){var result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);var g=result.createGraphics();g.drawImage(source,0,0,null);g.dispose();return result;}
    public static BufferedImage readSource(Path file)throws IOException{
        long bytes=Files.size(file);if(bytes>128_000_000)throw new IOException("Image exceeds 128 MB.");
        BufferedImage source;
        try(var input=ImageIO.createImageInputStream(file.toFile())){
            if(input==null)throw new IOException("Cannot read image.");
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Choose a PNG, JPEG or WebP image.");
            var reader=readers.next();
            try{reader.setInput(input);if(!java.util.Set.of("png","jpeg","jpg","webp").contains(reader.getFormatName().toLowerCase(java.util.Locale.ROOT)))throw new IOException("Choose a PNG, JPEG or WebP image.");
                int w=reader.getWidth(0),h=reader.getHeight(0);boolean atlas=w==h*2&&w>=64&&w<=8192&&w%64==0;
                if(!atlas&&bytes>8_000_000)throw new IOException("Choose an image smaller than 8 MB.");
                if(w<1||h<1||w>(atlas?8192:4096)||h>4096||(long)w*h>(atlas?33_554_432:8_000_000))throw new IOException("Image is too large (maximum 4096 pixels per side / 8 MP).");source=reader.read(0);
            }finally{reader.dispose();}
        }
        return source;
    }
    public static boolean isAtlas(BufferedImage source){int w=source.getWidth();return w==source.getHeight()*2&&w>=64&&w<=8192&&w%64==0;}
    public static BufferedImage importImage(Path file)throws IOException{var source=readSource(file);return isAtlas(source)?copy(source):fit(source,0,0,source.getWidth(),source.getHeight());}
    /** Fit the selected source rectangle without downsampling it. */
    public static BufferedImage fit(BufferedImage source,double x,double y,double w,double h)throws IOException{
        int unit=Math.max(1,(int)Math.ceil(Math.max(w/10,h/16)));
        if(unit>128)throw new IOException("Selection exceeds 1280 x 2048 pixels. Crop a smaller area to retain full detail.");
        var atlas=new BufferedImage(64*unit,32*unit,BufferedImage.TYPE_INT_ARGB);var g=atlas.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double scale=Math.min(10d*unit/w,16d*unit/h);
        for(int face:new int[]{1,12}){
            g.setClip(new java.awt.geom.Rectangle2D.Double(face*unit+(10*unit-w*scale)/2,unit+(16*unit-h*scale)/2,w*scale,h*scale));
            var transform=new java.awt.geom.AffineTransform();transform.translate(face*unit+(10*unit-w*scale)/2-x*scale,unit+(16*unit-h*scale)/2-y*scale);transform.scale(scale,scale);g.drawImage(source,transform,null);
        }
        g.dispose();return atlas;
    }
    public static void upload(Identifier id,BufferedImage image)throws IOException{
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
        var nativeImage=NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
        Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"Aeternum cape",nativeImage));
    }
    public static void save(BufferedImage image)throws IOException{
        Files.createDirectories(FILE.getParent());Path temp=FILE.resolveSibling("cape.tmp.png");ImageIO.write(image,"png",temp.toFile());
        try{Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);}
        saved=copy(image);upload(TEXTURE,saved);loaded=true;available=true;
    }
}
