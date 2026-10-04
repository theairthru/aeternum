package dev.aeternum;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
final class LyricsCredits {
    record Row(FormattedCharSequence text,String url){}
    private static java.lang.ref.WeakReference<LyricsController.Result> last=new java.lang.ref.WeakReference<>(null);private static int width;private static List<Row> rows=List.of();
    static List<Row> rows(LyricsController.Result result,int w){
        if(last.get()==result&&width==w)return rows;
        List<Row> next=new ArrayList<>();for(var credit:result.credits())for(var text:Minecraft.getInstance().font.split(Component.literal(credit.label()),w))next.add(new Row(text,credit.url()));
        last=new java.lang.ref.WeakReference<>(result);width=w;rows=List.copyOf(next);return rows;
    }
    static void open(String url){if(!url.isBlank())net.minecraft.util.Util.getPlatform().openUri(url);}
}
