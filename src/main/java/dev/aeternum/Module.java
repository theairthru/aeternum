package dev.aeternum;

import net.minecraft.client.KeyMapping;

public enum Module {
    ARMOUR("Armour status", "Combat", "Hotbar slots, durability and break warnings", true, true),
    KEYSTROKES("Keystrokes", "HUD", "Movement keys, mouse buttons and CPS", true, true),
    CLOCK("Clock", "HUD", "Local time in digital or analogue form", true, true),
    FPS("FPS", "HUD", "Frames per second", true, true),
    PING("Ping", "HUD", "Connection latency", false, true),
    COORDINATES("Coordinates", "HUD", "Position and facing direction", false, true),
    EFFECTS("Potion effects", "HUD", "Active effects and remaining time", false, true),
    LOW_HEALTH("Health warning", "Combat", "A sound and your own warning message", true, true),
    FREELOOK("Freelook", "Camera", "Hold to orbit without turning your player", true, false),
    FREECAM("Freecam", "Camera", "Toggle an independent flying camera", false, false),
    XRAY("X-ray", "World", "Choose visible blocks and background opacity", false, false),
    HITBOXES("Hitboxes", "Combat", "Coloured visual boxes by entity category", false, false),
    ZOOM("Zoom", "Camera", "Hold for a closer look", true, false),
    SPRINT("Toggle sprint", "Movement", "Sprint automatically while moving forward", false, false),
    FULLBRIGHT("Fullbright", "World", "Brighten dark places", false, false),
    PERFORMANCE("Performance", "System", "Entity distance and occlusion culling", false, false),
    CAPE("Custom cape", "Cosmetic", "Paint or import a cape visible only to you", false, false),
    LYRICS("Lyrics", "HUD", "Independent synced lyrics with smooth scrolling", true, true),
    MEDIA("Media controls", "System", "Control the active desktop media session", true, true);

    public final String title, category, description;
    public final boolean defaultEnabled, overlay;
    public KeyMapping key;
    Module(String title, String category, String description, boolean enabled, boolean overlay) {
        this.title=title; this.category=category; this.description=description;
        this.defaultEnabled=enabled; this.overlay=overlay;
    }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public Config.State state() { return Aeternum.config.modules.get(id()); }
    public boolean enabled() { return state().enabled; }
    public void toggle() { state().enabled=!state().enabled; Aeternum.changed(this); }
}

