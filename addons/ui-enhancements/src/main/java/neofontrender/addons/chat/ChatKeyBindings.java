package neofontrender.addons.chat;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import org.lwjgl.input.Keyboard;

/**
 * Chat clipboard keybindings. Minecraft 1.7.10 KeyBinding has no modifier support and key
 * events are not dispatched while a screen is open, so Ctrl+letter combos are edge-detected
 * from the client tick while the vanilla chat screen is displayed. The vanilla text field
 * already handles Ctrl+C/X/V/A itself; these bindings add copy-from-history and keep the
 * context menu state in sync.
 */
public final class ChatKeyBindings {
    private static final String CATEGORY = "key.categories.neofontrender_ui_enhancements.chat";

    private static final KeyBinding COPY = binding("copy", Keyboard.KEY_C);
    private static final KeyBinding CUT = binding("cut", Keyboard.KEY_X);
    private static final KeyBinding PASTE = binding("paste", Keyboard.KEY_V);
    private static final KeyBinding SELECT_ALL = binding("select_all", Keyboard.KEY_A);
    private static final KeyBinding[] ALL = {COPY, CUT, PASTE, SELECT_ALL};
    private static final ChatKeyBindings INSTANCE = new ChatKeyBindings();

    private static boolean registered;
    private static boolean handledCurrentEvent;

    private final boolean[] previousDown = new boolean[ALL.length];

    private ChatKeyBindings() {}

    public static void register() {
        if (registered) return;
        registered = true;
        for (KeyBinding binding : ALL) ClientRegistry.registerKeyBinding(binding);
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean[] down = new boolean[ALL.length];
        for (int index = 0; index < ALL.length; index++) {
            down[index] = Keyboard.isKeyDown(ALL[index].getKeyCode());
        }
        handledCurrentEvent = false;
        try {
            if (!EnhancedChatFeatures.copySelection() || !(minecraft.currentScreen instanceof GuiChat)
                    || !GuiScreen.isCtrlKeyDown()) return;
            GuiTextField input = ((AccessorGuiChatFeatures) minecraft.currentScreen).nfrUi$getInputField();
            boolean focusedInput = input != null && input.isFocused();

            if (edge(down, 0)) {
                if (focusedInput && ChatContextMenu.hasSelection(input)) {
                    // The vanilla text field exports the selection to the clipboard by itself.
                    handledCurrentEvent = true;
                } else {
                    handledCurrentEvent = ChatCopyController.INSTANCE.copySelectedHistory();
                }
            } else if (edge(down, 1) && focusedInput && ChatContextMenu.hasSelection(input)) {
                handledCurrentEvent = true;
            } else if (edge(down, 2) && focusedInput) {
                handledCurrentEvent = true;
            } else if (edge(down, 3) && focusedInput) {
                handledCurrentEvent = true;
            }

            if (handledCurrentEvent) ChatContextMenu.INSTANCE.close();
        } finally {
            System.arraycopy(down, 0, previousDown, 0, down.length);
        }
    }

    private boolean edge(boolean[] down, int index) {
        return down[index] && !previousDown[index];
    }

    public static boolean handledCurrentEvent() {
        return handledCurrentEvent;
    }

    public static String copyDisplayName() { return displayName(COPY); }
    public static String cutDisplayName() { return displayName(CUT); }
    public static String pasteDisplayName() { return displayName(PASTE); }
    public static String selectAllDisplayName() { return displayName(SELECT_ALL); }

    private static String displayName(KeyBinding binding) {
        return "Ctrl+" + GameSettings.getKeyDisplayString(binding.getKeyCode());
    }

    private static KeyBinding binding(String id, int keyCode) {
        return new KeyBinding("key.neofontrender_ui_enhancements.chat." + id, keyCode, CATEGORY);
    }
}
