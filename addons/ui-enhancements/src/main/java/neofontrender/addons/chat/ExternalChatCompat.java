package neofontrender.addons.chat;

import cpw.mods.fml.common.Loader;
import net.minecraft.client.gui.GuiTextField;

import java.util.Map;
import java.util.WeakHashMap;

public final class ExternalChatCompat {
    private static final String SALUTATION_CHAT_PACKAGE =
            "speiger.src.salutation.client.gui.chat.";
    private static final Map<GuiTextField, InputGeometry> SALUTATION_INPUTS =
            new WeakHashMap<>();

    private ExternalChatCompat() {}

    public static boolean tabbyChatLoaded() {
        return Loader.isModLoaded("tabbychat2");
    }

    /**
     * Salutation wraps GuiChat with its own command-completion screen. Use a name check instead of
     * linking its classes so this addon remains fully optional when Salutation is not installed.
     */
    public static boolean isSalutationChatScreen(Object screen) {
        return screen != null
                && screen.getClass().getName().startsWith(SALUTATION_CHAT_PACKAGE)
                && Loader.isModLoaded("salutation");
    }

    public static void updateSalutationInput(
            GuiTextField textField, int x, int y, int width, int height, float scale) {
        if (textField == null) return;
        SALUTATION_INPUTS.put(textField, new InputGeometry(x, y, width, height, scale));
    }

    public static void removeSalutationInput(GuiTextField textField) {
        if (textField != null) SALUTATION_INPUTS.remove(textField);
    }

    public static InputGeometry getSalutationInput(GuiTextField textField) {
        return textField == null ? null : SALUTATION_INPUTS.get(textField);
    }

    public static final class InputGeometry {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public final float scale;

        private InputGeometry(int x, int y, int width, int height, float scale) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.scale = scale;
        }
    }
}
