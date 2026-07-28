package neofontrender.addons.chat;

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
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        handledCurrentEvent = false;
        if (!EnhancedChatFeatures.copySelection() || !(event.getGui() instanceof GuiChat)
                || !Keyboard.getEventKeyState()) return;
        int keyCode = Keyboard.getEventKey();
        GuiTextField input = ((AccessorGuiChatFeatures) event.getGui()).nfrUi$getInputField();
        boolean focusedInput = input != null && input.isFocused();

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

    private static KeyBinding binding(String id, KeyModifier modifier, int keyCode) {
        return new KeyBinding("key.neofontrender_ui_enhancements.chat." + id,
                KeyConflictContext.GUI, modifier, keyCode, CATEGORY);
    }
}
