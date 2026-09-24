package neofontrender.addons.tooltips;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.api.config.NfrConfigFile;
import neofontrender.addons.ui.UiEnhancementsConfig;
import org.lwjgl.input.Keyboard;

/** Item Zoom controls matching the original 1.12 mod. */
final class ItemZoomKeyBindings {
    private static final String CATEGORY = "key.categories.neofontrender_ui_enhancements";

    static final KeyBinding TOGGLE = new KeyBinding(
            "key.neofontrender_ui_enhancements.item_zoom.toggle",
            KeyConflictContext.GUI, KeyModifier.SHIFT, Keyboard.KEY_Z, CATEGORY);
    static final KeyBinding HOLD = new KeyBinding(
            "key.neofontrender_ui_enhancements.item_zoom.hold",
            KeyConflictContext.GUI, KeyModifier.NONE, Keyboard.KEY_NONE, CATEGORY);

    private static final ItemZoomKeyBindings INSTANCE = new ItemZoomKeyBindings();
    private static final String TOGGLED_ENABLED_KEY = "tooltip.zoomOverlay.toggled.enabled";
    private static boolean registered;
    private static boolean toggledEnabled = true;
    private static NfrConfigFile config;

    private ItemZoomKeyBindings() {}

    static void register() {
        if (registered) return;
        registered = true;
        config = UiEnhancementsConfig.file();
        config.define(TOGGLED_ENABLED_KEY, true,
                "Runtime Item Zoom toggle state; Shift+Z switches it in GUI screens.");
        toggledEnabled = config.getBoolean(TOGGLED_ENABLED_KEY, true);
        config.save();
        ClientRegistry.registerKeyBinding(TOGGLE);
        ClientRegistry.registerKeyBinding(HOLD);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    static boolean isHoldDown() {
        return HOLD.isKeyDown();
    }

    static boolean isActive() {
        return TooltipConfig.zoomOverlayEnabled && (toggledEnabled || isHoldDown());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGuiKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Post event) {
        if (!Keyboard.getEventKeyState()) return;
        int eventKey = Keyboard.getEventKey();
        if (TOGGLE.isActiveAndMatches(eventKey)) {
            toggledEnabled = !toggledEnabled;
            if (config != null) {
                config.set(TOGGLED_ENABLED_KEY, toggledEnabled).save();
            }
            event.setCanceled(true);
        }
    }
}
