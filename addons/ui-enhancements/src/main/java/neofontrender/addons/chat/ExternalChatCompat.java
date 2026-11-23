package neofontrender.addons.chat;

import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.common.Loader;
import neofontrender.addons.bundled.BundledModRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class ExternalChatCompat {
    private static final Map<GuiTextField, InputGeometry> SALUTATION_INPUTS =
            new WeakHashMap<>();

    private ExternalChatCompat() {}

    public static boolean tabbyChatLoaded() {
        return Loader.isModLoaded("tabbychat2") && !BundledModRegistry.isTabbyChatBundled();
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
