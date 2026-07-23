package neofontrender.addons.chat;

import net.minecraftforge.fml.common.Loader;

public final class ExternalChatCompat {
    private static final String SALUTATION_CHAT_PACKAGE =
            "speiger.src.salutation.client.gui.chat.";

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
}
