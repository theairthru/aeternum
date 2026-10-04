package dev.aeternum;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public final class Xray {
    private static volatile Set<String> visible=Set.of();
    public static void refresh(){visible=Set.copyOf(Aeternum.config.xrayBlocks);}
    public static boolean active(){return Aeternum.config!=null&&Module.XRAY.enabled();}
    public static boolean selected(BlockState state){return visible.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());}
    public static boolean hidden(BlockState state){return active()&&!selected(state);}
}
