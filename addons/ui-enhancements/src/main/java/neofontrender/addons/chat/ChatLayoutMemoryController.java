package neofontrender.addons.chat;

import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.gui.ChatBox;
import mnm.mods.util.ILocation;
import mnm.mods.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.ui.UiEnhancementsConfig;

import java.util.Collections;
import java.util.Map;

/** Applies remembered bounds before any screen clamping; writes only settled changes. */
public final class ChatLayoutMemoryController {
    private static final String ENABLED = "chat.tabby.layout.memory.enabled";
    private static final String PROFILES = "chat.tabby.layout.memory.profiles";
    private static final ChatLayoutMemory MEMORY = new ChatLayoutMemory();
    private static boolean enabled;

    private ChatLayoutMemoryController() {}

    public static void load() {
        var file = UiEnhancementsConfig.file();
        file.define(ENABLED, false, "Remember chat position and size per display resolution, GUI scale and chat scale.");
        file.define(PROFILES, Collections.emptyList(), "width:height:guiScale:chatScale1000:x:y:chatWidth:chatHeight");
        enabled = file.getBoolean(ENABLED, false);
        MEMORY.replace(ChatLayoutMemory.decode(file.getStringList(PROFILES, Collections.emptyList())));
        MEMORY.resetViewport();
    }

    public static boolean enabled() { return enabled; }
    static Map<ChatLayoutMemory.Viewport, ChatLayoutMemory.Bounds> snapshot() { return MEMORY.snapshot(); }

    static void configure(boolean value, Map<ChatLayoutMemory.Viewport, ChatLayoutMemory.Bounds> profiles) {
        MEMORY.replace(profiles);
        if (enabled != value) MEMORY.resetViewport();
        enabled = value;
        save();
    }

    static ChatLayoutMemory.Viewport viewport() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0 || TabbyChat.getInstance().getChatGui() == null)
            return null;
        float chatScale = TabbyChat.getInstance().getChatGui().getChatScale();
        if (!Float.isFinite(chatScale) || chatScale <= 0) return null;
        return new ChatLayoutMemory.Viewport(mc.displayWidth, mc.displayHeight,
                new ScaledResolution(mc).getScaleFactor(),
                Math.max(1, Math.round(chatScale * 1000)));
    }

    public static void beforeLayout(ChatBox box) {
        if (!enabled) return;
        var viewport = viewport();
        if (viewport == null) return;
        var saved = MEMORY.enter(viewport);
        if (saved != null) {
            box.setLocation(new Location(saved.x(), saved.y(), saved.width(), saved.height()));
            box.getChatArea().markDirty();
        }
    }

    public static void afterLayout(ChatBox box, boolean dragging) {
        if (!enabled || viewport() == null) return;
        if (MEMORY.observe(bounds(box), System.nanoTime(), dragging)) save();
    }

    private static ChatLayoutMemory.Bounds bounds(ChatBox box) {
        ILocation p = box.getLocation();
        return new ChatLayoutMemory.Bounds(p.getXPos(), p.getYPos(), p.getWidth(), p.getHeight());
    }

    public static void frame(ChatBox box) {
        if (!enabled) return;
        var viewport = viewport();
        if (viewport == null) return;
        if (MEMORY.needsLayout(viewport, bounds(box))) box.updateComponent();
        else afterLayout(box, box.isLayoutDragging());
    }

    public static void dragFinished(ChatBox box) {
        if (!enabled || viewport() == null) return;
        box.updateComponent();
        if (MEMORY.rememberNow(bounds(box))) save();
    }

    private static void save() {
        UiEnhancementsConfig.file().set(ENABLED, enabled)
                .set(PROFILES, ChatLayoutMemory.encode(MEMORY.snapshot())).save();
    }
}
