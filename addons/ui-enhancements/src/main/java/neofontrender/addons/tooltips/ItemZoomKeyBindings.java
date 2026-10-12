package neofontrender.addons.tooltips;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;
import org.lwjgl.input.Keyboard;

/** Item Zoom controls matching the original 1.12 mod. */
final class ItemZoomKeyBindings {
    private static final String CATEGORY = "key.categories.neofontrender_ui_enhancements";

    // 1.7.10's KeyBinding has no conflict context or modifier field, so the modifiers main
    // declares through KeyConflictContext/KeyModifier are matched in onGuiKeyInput instead.
    static final KeyBinding TOGGLE = new KeyBinding(
            "key.neofontrender_ui_enhancements.item_zoom.toggle", Keyboard.KEY_Z, CATEGORY);
    static final KeyBinding HOLD = new KeyBinding(
            "key.neofontrender_ui_enhancements.item_zoom.hold", Keyboard.KEY_NONE, CATEGORY);
    static final KeyBinding MODE = new KeyBinding(
            "key.neofontrender_ui_enhancements.item_zoom.mode", Keyboard.KEY_Z, CATEGORY);

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
        ClientRegistry.registerKeyBinding(MODE);
        // 1.7.10 has no GuiScreenEvent.KeyboardInputEvent. FML publishes every key press of the
        // tick loop, GUI screens included, on its own bus.
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    static boolean isHoldDown() {
        int keyCode = HOLD.getKeyCode();
        return keyCode != Keyboard.KEY_NONE && Keyboard.isKeyDown(keyCode);
    }

    static boolean isActive() {
        return TooltipConfig.zoomOverlayEnabled && (toggledEnabled || isHoldDown());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGuiKeyInput(InputEvent.KeyInputEvent event) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof GuiContainer)) return;
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
        int eventKey = Keyboard.getEventKey();
        boolean control = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        // The FML key event cannot be consumed, so the screen still sees the key; no vanilla
        // container screen binds Z, Shift+Z or Ctrl+Z, which main also guarantees by cancelling.
        if (control && !shift && MODE.getKeyCode() == eventKey) {
            ItemStack stack = ItemZoomOverlay.hoveredStack(screen);
            if (stack == null || !ItemZoomOverlay.allowed(stack)) return;
            ItemZoomPresentation.toggleMode(ItemZoomPresentation.category(stack));
            TooltipConfig.save();
        } else if (shift && !control && TOGGLE.getKeyCode() == eventKey) {
            toggledEnabled = !toggledEnabled;
            if (config != null) {
                config.set(TOGGLED_ENABLED_KEY, toggledEnabled).save();
            }
        }
    }
}
