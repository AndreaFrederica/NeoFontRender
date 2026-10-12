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
import neofontrender.addons.vendor.tabbychat.ChatManager;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import org.lwjgl.input.Keyboard;

/**
 * Chat clipboard and panel keybindings. Minecraft 1.7.10 KeyBinding has no modifier support and
 * key events are not dispatched to Forge while a screen is open, so Ctrl+letter combos are
 * edge-detected from the client tick while the vanilla chat screen is displayed. The vanilla text
 * field already handles Ctrl+C/X/V/A itself; these bindings add copy-from-history plus the search
 * and history panels, and keep the context menu state in sync.
 */
public final class ChatKeyBindings {
    private static final String CATEGORY = "key.categories.neofontrender_ui_enhancements.chat";

    private static final int COPY_INDEX = 0;
    private static final int CUT_INDEX = 1;
    private static final int PASTE_INDEX = 2;
    private static final int SELECT_ALL_INDEX = 3;
    private static final int SEARCH_INDEX = 4;
    private static final int HISTORY_INDEX = 5;

    private static final KeyBinding COPY = binding("copy", Keyboard.KEY_C);
    private static final KeyBinding CUT = binding("cut", Keyboard.KEY_X);
    private static final KeyBinding PASTE = binding("paste", Keyboard.KEY_V);
    private static final KeyBinding SELECT_ALL = binding("select_all", Keyboard.KEY_A);
    private static final KeyBinding SEARCH = binding("search", Keyboard.KEY_F);
    private static final KeyBinding HISTORY = binding("history", Keyboard.KEY_H);
    private static final KeyBinding HUD_INTERACT = binding("hud_interact", Keyboard.KEY_GRAVE);
    private static final KeyBinding[] ALL = {COPY, CUT, PASTE, SELECT_ALL, SEARCH, HISTORY};
    private static final ChatKeyBindings INSTANCE = new ChatKeyBindings();

    private static boolean registered;
    private static boolean handledCurrentEvent;

    private final boolean[] previousDown = new boolean[ALL.length];

    private ChatKeyBindings() {}

    public static void register() {
        if (registered) return;
        registered = true;
        for (KeyBinding binding : ALL) ClientRegistry.registerKeyBinding(binding);
        ClientRegistry.registerKeyBinding(HUD_INTERACT);
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    public static boolean hudInteractionDown() {
        return Keyboard.isKeyDown(HUD_INTERACT.getKeyCode());
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
            if (!(minecraft.currentScreen instanceof GuiChat)) return;
            GuiChat chatScreen = (GuiChat) minecraft.currentScreen;
            GuiTextField input = ((AccessorGuiChatFeatures) chatScreen).nfrUi$getInputField();
            boolean focusedInput = input != null && input.isFocused();

            // The search panel owns the keyboard while it is displayed: 1.7.10 cannot cancel the
            // key for the screen, so its own handling replaces the shortcuts for this tick.
            if (ChatSearchController.INSTANCE.isOpen()) {
                handledCurrentEvent = ChatSearchController.INSTANCE.handleKeyboard();
                if (handledCurrentEvent) ChatContextMenu.INSTANCE.close();
                return;
            }
            if (!GuiScreen.isCtrlKeyDown()) return;
            if (edge(down, SEARCH_INDEX) && EnhancedChatConfig.messageSearch) {
                ChatSearchController.INSTANCE.open(chatScreen);
                handledCurrentEvent = true;
                return;
            }
            if (edge(down, HISTORY_INDEX) && EnhancedChatConfigAccess.tabbedChatEnabled()) {
                ChatHistoryScreen.open();
                handledCurrentEvent = true;
                return;
            }
            if (!EnhancedChatFeatures.copySelection()) return;

            if (edge(down, COPY_INDEX)) {
                if (focusedInput && ChatContextMenu.hasSelection(input)) {
                    // The vanilla text field exports the selection to the clipboard by itself.
                    handledCurrentEvent = true;
                } else {
                    handledCurrentEvent = ChatCopyController.INSTANCE.copySelectedHistory();
                }
            } else if (edge(down, CUT_INDEX) && focusedInput && ChatContextMenu.hasSelection(input)) {
                handledCurrentEvent = true;
            } else if (edge(down, PASTE_INDEX) && focusedInput) {
                handledCurrentEvent = true;
            } else if (edge(down, SELECT_ALL_INDEX) && focusedInput) {
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

    public static boolean removePrivateCommandBlock(GuiTextField field) {
        if (field == null) return false;
        String text = field.getText();
        if (text == null || text.isEmpty()) return false;
        String template = EnhancedChatConfig.privateMessageCommand == null
                ? "/msg" : EnhancedChatConfig.privateMessageCommand.trim();
        if (!template.startsWith("/")) template = "/" + template;
        if (!text.startsWith(template + " ")) return false;
        field.setText(text.substring(template.length() + 1));
        field.setCursorPosition(0);
        return true;
    }

    /** Clears the PM draft only after GuiChat has copied and sent its text. */
    public static void resetPrivateInputAfterSend(GuiTextField input) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled() || input == null) return;
        ChatManager manager = TabbyChat.getInstance().getChat();
        if (manager == null || !manager.getActiveChannel().isPm()) return;
        manager.clearActiveDraft();
    }

    public static String copyDisplayName() { return displayName(COPY); }
    public static String cutDisplayName() { return displayName(CUT); }
    public static String pasteDisplayName() { return displayName(PASTE); }
    public static String selectAllDisplayName() { return displayName(SELECT_ALL); }
    public static String searchDisplayName() { return displayName(SEARCH); }

    private static String displayName(KeyBinding binding) {
        return "Ctrl+" + GameSettings.getKeyDisplayString(binding.getKeyCode());
    }

    private static KeyBinding binding(String id, int keyCode) {
        return new KeyBinding("key.neofontrender_ui_enhancements.chat." + id, keyCode, CATEGORY);
    }
}
