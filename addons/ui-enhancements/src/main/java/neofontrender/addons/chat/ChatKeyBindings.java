package neofontrender.addons.chat;

import mnm.mods.tabbychat.ChatManager;
import mnm.mods.tabbychat.TabbyChat;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import org.lwjgl.input.Keyboard;

public final class ChatKeyBindings {
    private static final String CATEGORY = "key.categories.neofontrender_ui_enhancements.chat";
    private static final ChatKeyBindings INSTANCE = new ChatKeyBindings();

    private static final KeyBinding COPY = binding("copy", KeyModifier.CONTROL, Keyboard.KEY_C);
    private static final KeyBinding CUT = binding("cut", KeyModifier.CONTROL, Keyboard.KEY_X);
    private static final KeyBinding PASTE = binding("paste", KeyModifier.CONTROL, Keyboard.KEY_V);
    private static final KeyBinding SELECT_ALL = binding("select_all", KeyModifier.CONTROL, Keyboard.KEY_A);
    private static final KeyBinding SEARCH = binding("search", KeyModifier.CONTROL, Keyboard.KEY_F);
    private static final KeyBinding HUD_INTERACT = new KeyBinding(
            "key.neofontrender_ui_enhancements.chat.hud_interact", KeyConflictContext.IN_GAME,
            Keyboard.KEY_LMENU, CATEGORY);

    private static boolean registered;
    private static boolean handledCurrentEvent;

    private ChatKeyBindings() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ClientRegistry.registerKeyBinding(COPY);
        ClientRegistry.registerKeyBinding(CUT);
        ClientRegistry.registerKeyBinding(PASTE);
        ClientRegistry.registerKeyBinding(SELECT_ALL);
        ClientRegistry.registerKeyBinding(SEARCH);
        ClientRegistry.registerKeyBinding(HUD_INTERACT);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        handledCurrentEvent = false;
        if (!(event.getGui() instanceof GuiChat) || !Keyboard.getEventKeyState()) return;
        int keyCode = Keyboard.getEventKey();
        if (ChatSearchController.INSTANCE.isOpen()) {
            handledCurrentEvent = ChatSearchController.INSTANCE.handleKeyboard();
            if (handledCurrentEvent) event.setCanceled(true);
            return;
        }
        if (SEARCH.isActiveAndMatches(keyCode) && EnhancedChatConfig.messageSearch) {
            ChatSearchController.INSTANCE.open((GuiChat) event.getGui());
            handledCurrentEvent = true;
            event.setCanceled(true);
            return;
        }
        GuiTextField input = ((AccessorGuiChatFeatures) event.getGui()).nfrUi$getInputField();
        boolean focusedInput = input != null && input.isFocused();
        if (MentionCompletionController.handleKey(input, keyCode)) {
            handledCurrentEvent = true;
            event.setCanceled(true);
            return;
        }
        if (ChatCommandCompletionController.handleKey(input, keyCode)) {
            handledCurrentEvent = true;
            event.setCanceled(true);
            return;
        }
        if (!EnhancedChatFeatures.copySelection()) return;

        if (COPY.isActiveAndMatches(keyCode)) {
            if (focusedInput && ChatContextMenu.hasSelection(input)) {
                ChatContextMenu.copyInput(input);
                handledCurrentEvent = true;
            } else {
                handledCurrentEvent = ChatCopyController.INSTANCE.copySelectedHistory();
            }
        } else if (CUT.isActiveAndMatches(keyCode) && focusedInput
                && ChatContextMenu.hasSelection(input)) {
            ChatContextMenu.cutInput(input);
            handledCurrentEvent = true;
        } else if (PASTE.isActiveAndMatches(keyCode) && focusedInput) {
            ChatContextMenu.pasteInput(input);
            handledCurrentEvent = true;
        } else if (SELECT_ALL.isActiveAndMatches(keyCode) && focusedInput) {
            ChatContextMenu.selectAllInput(input);
            handledCurrentEvent = true;
        }

        if (handledCurrentEvent) {
            ChatContextMenu.INSTANCE.close();
            event.setCanceled(true);
        }
    }

    public static boolean handledCurrentEvent() {
        return handledCurrentEvent;
    }

    public static String copyDisplayName() { return COPY.getDisplayName(); }
    public static String cutDisplayName() { return CUT.getDisplayName(); }
    public static String pasteDisplayName() { return PASTE.getDisplayName(); }
    public static String selectAllDisplayName() { return SELECT_ALL.getDisplayName(); }
    public static String searchDisplayName() { return SEARCH.getDisplayName(); }

    public static boolean hudInteractionDown() { return HUD_INTERACT.isKeyDown(); }

    public static boolean removePrivateCommandBlock(GuiTextField input) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()) return false;
        ChatManager manager = TabbyChat.getInstance().getChat();
        return manager != null && input != null && manager.removeActivePrivateCommandBlock();
    }

    /** Clears the PM draft only after GuiChat has copied and sent its text. */
    public static void resetPrivateInputAfterSend(GuiTextField input) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled() || input == null) return;
        ChatManager manager = TabbyChat.getInstance().getChat();
        if (manager == null || !manager.getActiveChannel().isPm()) return;
        manager.clearActiveDraft();
    }

    private static KeyBinding binding(String id, KeyModifier modifier, int keyCode) {
        return new KeyBinding("key.neofontrender_ui_enhancements.chat." + id,
                KeyConflictContext.GUI, modifier, keyCode, CATEGORY);
    }
}
