package dev.aeternum;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class SpicySettingsScreen extends Screen {
    private final Screen parent;private EditBox key;private String message="Use a publishable sl_pk_ key with native-client access enabled.";
    public SpicySettingsScreen(Screen parent){super(Component.literal("Spicy Lyrics and Spicetify"));this.parent=parent;}
    @Override protected void init(){
        int x=width/2-160,y=Math.max(45,height/2-100);
        key=new EditBox(font,x,y+24,320,22,Component.literal("Spicy Lyrics client key"));key.setMaxLength(256);key.addFormatter((s,start)->Component.literal("*".repeat(s.length())).getVisualOrderText());addRenderableWidget(key);
        addRenderableWidget(new UiButton(x,y+54,154,22,"Save client key",b->{String value=key.getValue().strip();if(!value.startsWith("sl_pk_")){message="Use the publishable client key beginning sl_pk_.";return;}Aeternum.config.spicyApiKey=value;Aeternum.config.save();Aeternum.media.lyrics.refresh();key.setValue("");message="Client key saved. Spicy Lyrics first, LRCLIB fallback.";}));
        addRenderableWidget(new UiButton(x+166,y+54,154,22,"Remove client key",b->{Aeternum.config.spicyApiKey="";Aeternum.config.save();Aeternum.media.lyrics.refresh();message="Client key removed. LRCLIB remains available.";}));
        addRenderableWidget(new UiButton(x,y+87,320,22,"Copy Spicetify pairing code",b->{minecraft.keyboardHandler.setClipboard(SpotifyBridge.pair());message="Copied. Paste into Spotify profile menu > Connect Aeternum.";}));
        addRenderableWidget(new UiButton(x,y+120,320,22,"Disconnect companion",b->{Aeternum.config.spotifyBridgeEnabled=false;Aeternum.config.spotifyPairToken="";Aeternum.config.save();SpotifyBridge.ensureStarted();Aeternum.media.lyrics.refresh();message="Companion disconnected. Old pairing code revoked.";}));
        addRenderableWidget(new UiButton(x,y+153,154,22,"Developer dashboard",b->net.minecraft.util.Util.getPlatform().openUri("https://developers.spicylyrics.org/dashboard/applications")));
        addRenderableWidget(new UiButton(x+166,y+153,154,22,"Back",b->onClose()));
    }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float d){}
    @Override public void render(GuiGraphics g,int mx,int my,float d){ClientScreen.backdrop(g,width,height);int x=width/2-160,y=Math.max(45,height/2-100);g.drawString(font,"SPICY LYRICS / SPICETIFY",x,y-23,0xffeeeeee,false);g.drawString(font,"Client key: "+(Aeternum.config.spicyApiKey.isBlank()?"not set":"saved locally"),x,y+9,0xffbbbbbb,false);g.drawString(font,SpotifyBridge.current()!=null?"Spicetify connected":SpotifyBridge.status,x,y+185,0xffbbbbbb,false);g.drawString(font,font.plainSubstrByWidth(Aeternum.media.lyrics.providerStatus,320),x,y+197,0xffbbbbbb,false);int yy=y+211;for(var line:font.split(Component.literal(message),320)){g.drawString(font,line,x,yy,0xffcccccc,false);yy+=11;}super.render(g,mx,my,d);}
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
