package dev.aeternum;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.client.CameraType;

public class ClientSmokeTest implements FabricClientGameTest {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static void checkMediaAndImages(){
        try {
            var clock=new PlaybackClock();clock.update("track",10,200,true,0);double before=clock.position(500_000_000L);clock.update("track",10.1,200,true,500_000_000L);check(clock.position(500_000_000L)>=before,"Clock went backwards on jitter");clock.update("track",2,200,true,1_000_000_000L);check(clock.position(1_000_000_000L)==2,"Real seek ignored");clock.update("track",2.1,200,false,1_100_000_000L);double paused=clock.position(2_000_000_000L);check(paused==clock.position(3_000_000_000L),"Paused clock advanced");
            var high=new java.awt.image.BufferedImage(600,960,java.awt.image.BufferedImage.TYPE_INT_ARGB);high.setRGB(300,480,0xff55aaee);var fitted=CapeStore.fit(high,0,0,600,960);check(fitted.getWidth()==3840&&fitted.getRGB(360,540)==0xff55aaee,"Cape was downsampled");
            var sub=new com.google.gson.JsonObject();sub.addProperty("subtitle_body","[00:01.50]Authored test line");sub.addProperty("subtitle_copyright","Test credit");var mxm=MusixmatchClient.parse(sub,true);check(mxm.timed().size()==1&&mxm.credit().contains("Test credit"),"Musixmatch LRC/credit parsing failed");sub.addProperty("restricted",1);check(MusixmatchClient.parse(sub,true).timed().isEmpty(),"Restricted lyrics displayed");
            var dir=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            var jpg=new java.awt.image.BufferedImage(64,32,java.awt.image.BufferedImage.TYPE_INT_RGB);
            jpg.setRGB(20,20,0xffaa33dd);var path=dir.resolve("cape-test.jpg");javax.imageio.ImageIO.write(jpg,"jpeg",path.toFile());
            check(CapeStore.importImage(path).getWidth()==64,"JPEG cape import failed");
            try(var in=ClientSmokeTest.class.getResourceAsStream("/cape-test.webp")){java.nio.file.Files.copy(in,dir.resolve("cape-test.webp"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
            check(CapeStore.importImage(dir.resolve("cape-test.webp")).getRGB(20,20)==0xffaf23d2,"WebP cape pixels changed");
            var mask=MediaController.roundedCover(jpg);check((mask.getRGB(0,0)>>>24)==0,"Cover corner is opaque");check((mask.getRGB(64,64)>>>24)==255,"Cover center lost opacity");
            boolean soft=false;for(int y=0;y<20;y++)for(int x=0;x<20;x++){int a=mask.getRGB(x,y)>>>24;if(a>0&&a<255)soft=true;}check(soft,"Cover edge is not antialiased");
            var lines=LyricsController.parseLrc("[offset:500]\n[00:01.00][00:03.50]Test line\n[00:02.00]Next line");
            check(lines.size()==3&&lines.get(0).seconds()==.5&&lines.get(2).seconds()==3,"LRC timing/offset parsing failed");
            var lyrics=new LyricsController();lyrics.result=new LyricsController.Result(lines,java.util.List.of(),"",false);check(lyrics.index(.2)==-1&&lyrics.index(1.5)==1,"Lyric selection failed");
            var snapshot=com.google.gson.JsonParser.parseString("{\"available\":true,\"title\":\"Test\",\"artist\":\"Artist\",\"album\":\"\",\"duration\":100,\"position\":12,\"playing\":false}").getAsJsonObject();
            Aeternum.media.acceptSnapshot(snapshot);check(!Aeternum.media.playing&&Aeternum.media.position()==12,"Pause snapshot was delayed");Aeternum.media.available=false;var clockField=MediaController.class.getDeclaredField("clock");clockField.setAccessible(true);((PlaybackClock)clockField.get(Aeternum.media)).reset();
        } catch(Exception e){throw new RuntimeException(e);}
    }
    private static LyricsController.Result spicyFixture(){try(var in=ClientSmokeTest.class.getResourceAsStream("/spicy-fixture.json")){return SpicyLyricsClient.parse(com.google.gson.JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());}catch(Exception e){throw new RuntimeException(e);}}
    private static void checkSpicyAndBridge(){try{
        var lyrics=spicyFixture();check(lyrics.timed().getFirst().text().equals("Light moves across the sky"),"Syllable spacing failed");check(lyrics.timed().getFirst().words().get(1).from()==6,"Word offsets failed");check(lyrics.credits().size()==3&&lyrics.credits().get(1).url().endsWith("/1"),"Community credits lost");
        String token=SpotifyBridge.pair();var http=java.net.http.HttpClient.newHttpClient();var uri=java.net.URI.create("http://127.0.0.1:38467/track");
        String body="{\"id\":\"1234567890123456789012\",\"title\":\"Authored track\",\"artist\":\"Test artist\",\"album\":\"\",\"duration\":120,\"position\":12,\"playing\":false}";
        var bad=java.net.http.HttpRequest.newBuilder(uri).POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();check(http.send(bad,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()==401,"Bridge accepted unauthenticated request");
        var good=java.net.http.HttpRequest.newBuilder(uri).header("Authorization","Bearer "+token).POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();check(http.send(good,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()==204,"Bridge rejected paired request");check(SpotifyBridge.current().id().equals("1234567890123456789012")&&SpotifyBridge.position()==12,"Bridge lost track/timing");
        var blocked=java.net.http.HttpRequest.newBuilder(uri).header("Authorization","Bearer "+token).header("Origin","https://example.invalid").POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();check(http.send(blocked,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode()==403,"Bridge accepted unexpected origin");
        Aeternum.config.spotifyBridgeEnabled=false;SpotifyBridge.ensureStarted();check(SpotifyBridge.current()==null,"Disconnected track retained");Aeternum.config.spotifyPairToken="";Aeternum.config.save();
    }catch(Exception e){throw new RuntimeException(e);}}
    @Override public void runTest(ClientGameTestContext c){
        c.runOnClient(mc->{Aeternum.config=new Config();Aeternum.config.normalize();Module.MEDIA.state().enabled=false;mc.options.guiScale().set(2);});
        c.runOnClient(mc->{checkMediaAndImages();checkSpicyAndBridge();});
        c.getInput().resizeWindow(1280,800);
        c.runOnClient(mc->check(java.util.Arrays.stream(mc.options.keyMappings).filter(k->k.getName().startsWith("key.aeternum.")).count()==1,"Only the menu key should be in Minecraft Controls"));
        c.setScreen(ClientScreen::new);c.waitTicks(3);c.takeScreenshot("aeternum-grid");
        c.setScreen(()->new SettingsScreen(new ClientScreen(),Module.ARMOUR));c.waitTicks(2);c.takeScreenshot("aeternum-armour-settings");
        c.setScreen(()->new BlockScreen(new ClientScreen()));c.waitTicks(2);
        c.getInput().typeChars("diamond");c.waitTicks(2);c.takeScreenshot("aeternum-block-picker");
        c.setScreen(()->null);
        try(var world=c.worldBuilder().create()){
            world.getClientWorld().waitForChunksRender();
            var origin=c.computeOnClient(mc->mc.player.blockPosition());
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(int xx=-2;xx<=2;xx++)for(int yy=0;yy<4;yy++)level.setBlockAndUpdate(origin.offset(xx,yy,3),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(0,1,5),net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(-3,0,4),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                var cow=net.minecraft.world.entity.EntityType.COW.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                check(cow!=null,"Could not create hitbox test entity");cow.setPos(origin.getX()+1.5,origin.getY(),origin.getZ()+2);cow.setNoAi(true);level.addFreshEntity(cow);
            });
            c.runOnClient(mc->{
                mc.player.setYRot(0);mc.player.setXRot(4);
                ItemStack helmet=new ItemStack(Items.DIAMOND_HELMET);helmet.setDamageValue(helmet.getMaxDamage()-8);
                mc.player.setItemSlot(EquipmentSlot.HEAD,helmet);mc.player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));mc.player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.DIAMOND_LEGGINGS));mc.player.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.DIAMOND_BOOTS));
                Module.HITBOXES.state().enabled=true;Module.FULLBRIGHT.state().enabled=true;Module.PERFORMANCE.state().enabled=true;
            });
            c.waitTicks(5);c.takeScreenshot("aeternum-hud");
            c.runOnClient(mc->check(Aeternum.renderedHitboxes>0,"Hitbox renderer did not emit geometry"));
            c.setScreen(()->new HudEditor(new ClientScreen()));c.waitTicks(2);c.takeScreenshot("aeternum-editor");
            c.runOnClient(mc->{
                var screen=(HudEditor)mc.screen;var r=Hud.bounds.get(Module.FPS);
                var button=new net.minecraft.client.input.MouseButtonInfo(0,0);
                check(screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(r.x()+5,r.y()+5,button),false),"Overlay click not accepted");
                int targetX=(screen.width-r.w())/2;
                screen.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(targetX+6,90,button),100,50);
                screen.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(targetX+6,90,button));
                check(Math.abs(Module.FPS.state().x-.5)<.015,"Overlay did not snap to centre");
                var loaded=Config.load();check(Math.abs(loaded.modules.get("fps").x-.5)<.015,"Overlay position did not persist");
                Module.FPS.state().x=.02;Module.FPS.state().y=.04;
            });
            c.setScreen(()->null);
            c.runOnClient(mc->{Aeternum.config.analogueClock=true;mc.player.setHealth(4);});
            c.waitTicks(2);c.runOnClient(mc->check(Aeternum.lowHealth,"Low-health warning not triggered"));c.takeScreenshot("aeternum-health-and-clock");
            c.runOnClient(mc->{mc.player.setHealth(20);Aeternum.config.analogueClock=false;});
            c.getInput().holdKey(Module.ZOOM.key);c.waitTicks(2);
            double magnification=Aeternum.config.zoomFactor;
            c.runOnClient(mc->check(mc.options.smoothCamera,"Zoom should enable cinematic camera"));
            c.getInput().scroll(2);c.waitTicks(1);
            check(Aeternum.config.zoomFactor>magnification,"Scroll did not increase zoom");
            c.getInput().releaseKey(Module.ZOOM.key);c.waitTicks(2);
            c.runOnClient(mc->check(!mc.options.smoothCamera,"Zoom did not restore cinematic setting"));
            c.getInput().holdKey(Module.FREELOOK.key);c.waitTicks(2);
            c.runOnClient(mc->{check(Aeternum.camera.freelookActive,"Freelook did not activate");float yaw=mc.player.getYRot();mc.player.turn(80,10);check(mc.player.getYRot()==yaw,"Freelook turned the body");});
            c.getInput().releaseKey(Module.FREELOOK.key);c.waitTicks(2);
            c.runOnClient(mc->{check(!Aeternum.camera.freelookActive,"Freelook did not release");check(mc.options.getCameraType()==CameraType.FIRST_PERSON,"Perspective not restored");});
            var body=c.computeOnClient(mc->mc.player.position());
            c.getInput().pressKey(Module.FREECAM.key);c.waitTicks(2);
            c.runOnClient(mc->check(Aeternum.camera.freecamActive,"Freecam did not activate"));
            var start=Aeternum.camera.position;
            c.getInput().holdKeyFor(o->o.keyRight,4);
            c.runOnClient(mc->check(Aeternum.camera.position.x<start.x,"Freecam right strafe is reversed"));
            c.getInput().holdKeyFor(o->o.keyUp,8);
            c.runOnClient(mc->{check(Aeternum.camera.position.distanceTo(start)>1,"Camera did not move");check(mc.player.position().distanceTo(body)<.1,"Freecam moved the body");});
            c.getInput().pressKey(Module.FREECAM.key);c.waitTicks(2);
            c.runOnClient(mc->{check(!Aeternum.camera.freecamActive,"Freecam did not stop");Module.XRAY.state().enabled=true;Aeternum.changed(Module.XRAY);});
            world.getClientWorld().waitForChunksRender();c.waitTicks(3);c.takeScreenshot("aeternum-xray");
            c.runOnClient(mc->{Aeternum.config.xrayOpacity=0;mc.levelRenderer.allChanged();});world.getClientWorld().waitForChunksRender();c.waitTicks(2);c.takeScreenshot("aeternum-xray-zero");
            c.runOnClient(mc->{Module.XRAY.state().enabled=false;Aeternum.changed(Module.XRAY);});
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(int xx=-2;xx<=2;xx++)for(int yy=0;yy<4;yy++)level.setBlockAndUpdate(origin.offset(xx,yy,3),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(origin.offset(0,1,5),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                for(var e:level.getAllEntities())if(e instanceof net.minecraft.world.entity.animal.cow.Cow)e.teleportTo(origin.getX()+.5,origin.getY(),origin.getZ()+6);
            });
            c.waitTicks(6);world.getClientWorld().waitForChunksRender();c.takeScreenshot("aeternum-hitboxes");
            c.runOnClient(mc->{
                mc.player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);mc.player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
                Module.EFFECTS.state().enabled=true;mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED,1200));
                // Fixed media fixture checks layout without controlling the user's player.
                try{
                    var field=MediaController.class.getDeclaredField("nextPoll");field.setAccessible(true);field.setLong(Aeternum.media,Long.MAX_VALUE);
                    var cover=new java.awt.image.BufferedImage(64,64,java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var brush=cover.createGraphics();brush.setColor(java.awt.Color.DARK_GRAY);brush.fillRect(0,0,64,64);brush.setColor(java.awt.Color.LIGHT_GRAY);brush.fillOval(12,12,40,40);brush.setColor(java.awt.Color.GRAY);brush.fillOval(24,24,16,16);brush.dispose();
                    var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(cover,"png",out);
                    var update=MediaController.class.getDeclaredMethod("updateArt",String.class);update.setAccessible(true);update.invoke(Aeternum.media,java.util.Base64.getEncoder().encodeToString(out.toByteArray()));
                }catch(Exception e){throw new RuntimeException(e);}
                Module.MEDIA.state().enabled=true;Aeternum.media.title="Example track";Aeternum.media.artist="Example artist";Aeternum.media.album="Example album";Aeternum.media.elapsed=72;Aeternum.media.duration=240;Aeternum.media.playing=true;Aeternum.media.measuredAt=System.currentTimeMillis();
                Aeternum.media.lyrics.result=new LyricsController.Result(java.util.List.of(new LyricsController.Line(0,"A quiet sky above the trees"),new LyricsController.Line(80,"Another day begins")),java.util.List.of(),"LRCLIB",false);
                Aeternum.config.analogueClock=true;
            });
            c.waitTicks(2);c.runOnClient(mc->{check(Aeternum.media.art!=null,"Album artwork did not load");check(Hud.bounds.containsKey(Module.LYRICS)&&Hud.bounds.get(Module.MEDIA).h()==83,"Lyrics are not independent");check(Shapes.submittedQuads<120,"HUD shapes exceeded fixed geometry budget: "+Shapes.submittedQuads);Aeternum.LOG.info("HUD shape geometry: {} quads",Shapes.submittedQuads);});c.takeScreenshot("aeternum-revised-overlays");
            c.runOnClient(mc->Module.MEDIA.state().enabled=false);
            c.setScreen(ClientScreen::new);c.waitTicks(2);c.takeScreenshot("aeternum-menu-in-world");
            c.runOnClient(mc->check(mc.screen.mouseScrolled(mc.screen.width/2d,mc.screen.height/2d,0,-10),"Grid should accept scrolling"));
            c.waitTicks(2);c.takeScreenshot("aeternum-grid-scrolled");c.setScreen(()->null);
            c.setScreen(()->new HudEditor(new ClientScreen()));c.waitTicks(2);
            c.runOnClient(mc->{
                var editor=(HudEditor)mc.screen;var r=Hud.bounds.get(Module.FPS);
                var left=new net.minecraft.client.input.MouseButtonInfo(0,0);
                double other=Module.CLOCK.state().scale;
                check(editor.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(r.x()+r.w()-2,r.y()+r.h()-2,left),false),"Resize handle missed");
                editor.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(r.x()+r.w()*1.5,r.y()+r.h()*1.5,left),30,15);
                editor.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(r.x()+r.w()*1.5,r.y()+r.h()*1.5,left));
                check(Math.abs(Module.FPS.state().scale-1.5)<.05,"Overlay did not resize");
                check(Module.CLOCK.state().scale==other,"Resizing FPS changed another overlay");
                check(Math.abs(Config.load().modules.get("fps").scale-1.5)<.05,"Overlay scale did not persist");
            });
            c.waitTicks(2);c.takeScreenshot("aeternum-resize-editor");
            c.runOnClient(mc->{var r=Hud.bounds.get(Module.FPS);mc.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(r.x()+4,r.y()+4,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);check(mc.screen instanceof SettingsScreen&&mc.screen.getTitle().getString().equals(Module.FPS.title),"Right click did not open matching settings");
                check(mc.screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget).map(w->((net.minecraft.client.gui.components.AbstractWidget)w).getMessage().getString()).noneMatch(s->s.contains("Open Aeternum menu")),"Menu bind should not be in module settings");});
            c.setScreen(()->new CapeScreen(new ClientScreen()));c.waitTicks(2);
            c.runOnClient(mc->{
                try{
                    var atlas=CapeStore.blank();for(int y=1;y<17;y++)for(int x=1;x<11;x++)atlas.setRGB(x,y,(x==1||x==10||y==1||y==16)?0xffeeeeee:0xff674997);
                    PathHolder.file=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("cape-test-import.png");javax.imageio.ImageIO.write(atlas,"png",PathHolder.file.toFile());
                    var painter=(CapeScreen)mc.screen;painter.importPath(PathHolder.file);check(mc.screen instanceof CapeImportScreen,"Import did not open crop editor");((CapeImportScreen)mc.screen).useAtlas();
                    var r=painter.canvasBounds();var click=new net.minecraft.client.input.MouseButtonEvent(r.x()+r.w()/2d+1,r.y()+r.h()/2d+1,new net.minecraft.client.input.MouseButtonInfo(0,0));
                    check(painter.mouseClicked(click,false),"Cape brush did not accept canvas click");painter.mouseReleased(click);painter.undo();
                    painter.mouseClicked(click,false);painter.mouseReleased(click);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            c.waitTicks(2);c.takeScreenshot("aeternum-cape-painter");
            c.runOnClient(mc->{((CapeScreen)mc.screen).saveAndWear();check(CapeStore.editable().getRGB(6,9)==0xffeeeeee,"Painted pixel was not saved");check(Module.CAPE.enabled(),"Save did not enable cape");check(mc.player.getSkin().cape()!=null&&mc.player.getSkin().cape().texturePath().equals(CapeStore.TEXTURE),"Local cape was not applied");
                var remote=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CapeCheck"));
                check(remote.getSkin().cape()==null||!remote.getSkin().cape().texturePath().equals(CapeStore.TEXTURE),"Custom cape leaked to another player");
                mc.setScreen(null);Module.HITBOXES.state().enabled=false;mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            });
            c.waitTicks(3);c.takeScreenshot("aeternum-cape-in-world");
            c.runOnClient(mc->{Module.CAPE.state().enabled=false;check(mc.player.getSkin().cape()==null||!mc.player.getSkin().cape().texturePath().equals(CapeStore.TEXTURE),"Cape toggle did not restore original skin");mc.options.setCameraType(CameraType.FIRST_PERSON);});
            c.setScreen(()->{
                var source=new java.awt.image.BufferedImage(600,960,java.awt.image.BufferedImage.TYPE_INT_RGB);var b=source.createGraphics();b.setPaint(new java.awt.GradientPaint(0,0,java.awt.Color.CYAN,600,960,java.awt.Color.MAGENTA));b.fillRect(0,0,600,960);b.setColor(java.awt.Color.WHITE);b.fillOval(120,200,360,360);b.dispose();return new CapeImportScreen(new CapeScreen(new ClientScreen()),source);
            });c.waitTicks(2);
            c.runOnClient(mc->{var screen=(CapeImportScreen)mc.screen;var r=screen.cropBounds();double[] before=screen.selection();screen.mouseScrolled(r.x()+5,r.y()+5,0,2);check(screen.selection()[2]<before[2],"Crop zoom did not change selection");var e=new net.minecraft.client.input.MouseButtonEvent(r.x()+r.w()/2d,r.y()+r.h()/2d,new net.minecraft.client.input.MouseButtonInfo(0,0));screen.mouseClicked(e,false);double oldX=screen.selection()[0];screen.mouseDragged(e,10,0);screen.mouseReleased(e);check(screen.selection()[0]!=oldX,"Crop drag did not move selection");});
            c.takeScreenshot("aeternum-cape-crop");
            c.runOnClient(mc->{((CapeImportScreen)mc.screen).apply();check(mc.screen instanceof CapeScreen,"Crop did not apply");mc.screen.onClose();mc.setScreen(null);Module.MEDIA.state().enabled=false;Module.LYRICS.state().enabled=true;Aeternum.config.lyricsSingleLine=true;});
            c.waitTicks(3);c.runOnClient(mc->{check(Hud.bounds.get(Module.LYRICS).h()==26,"Single lyric line has incorrect height");check(!Hud.bounds.containsKey(Module.MEDIA),"Media is not independently toggleable");});c.takeScreenshot("aeternum-lyrics-single-line");
            c.setScreen(()->new HudEditor(new ClientScreen()));c.waitTicks(2);
            c.runOnClient(mc->{var r=Hud.bounds.get(Module.LYRICS);mc.screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(r.x()+4,r.y()+4,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);check(mc.screen instanceof SettingsScreen&&mc.screen.getTitle().getString().equals("Lyrics"),"Lyrics right-click settings failed");});
            c.setScreen(()->new MusixmatchScreen(new ClientScreen()));c.waitTicks(2);c.takeScreenshot("aeternum-musixmatch-settings");c.setScreen(()->null);
            c.runOnClient(mc->{Aeternum.media.lyrics.result=spicyFixture();Aeternum.media.elapsed=3;Aeternum.media.playing=false;Aeternum.config.lyricsSingleLine=false;});c.waitTicks(3);c.takeScreenshot("aeternum-word-timed-lyrics");
            c.runOnClient(mc->check(Hud.bounds.get(Module.LYRICS).h()==79,"Credits are not visible in overlay"));
            c.setScreen(()->new LyricsScreen(new ClientScreen()));c.waitTicks(2);c.takeScreenshot("aeternum-lyrics-credits");
            c.setScreen(()->new SpicySettingsScreen(new ClientScreen()));c.waitTicks(2);c.takeScreenshot("aeternum-spicy-setup");c.setScreen(()->null);

        }
        c.runOnClient(mc->check(!Aeternum.camera.freecamActive,"Freecam survived disconnect"));
    }
    private static final class PathHolder {static java.nio.file.Path file;}
}





