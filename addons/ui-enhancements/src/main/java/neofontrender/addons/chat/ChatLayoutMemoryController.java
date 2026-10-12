package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.Location;
import neofontrender.addons.vendor.tabbychat.gui.ChatBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

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
        // 1.7.10's embedded TabbyChat throws from getInstance() before its host starts it, so the
        // "no chat GUI" test must ask whether the backend is running at all.
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0
                || !TabbyChat.isStarted() || TabbyChat.getInstance().getChatGui() == null)
            return null;
        float chatScale = TabbyChat.getInstance().getChatGui().getChatScale();
        if (!Float.isFinite(chatScale) || chatScale <= 0) return null;
        return new ChatLayoutMemory.Viewport(mc.displayWidth, mc.displayHeight,
                new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor(),
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
