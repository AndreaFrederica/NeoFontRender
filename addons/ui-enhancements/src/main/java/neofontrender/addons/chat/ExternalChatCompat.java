package neofontrender.addons.chat;

import cpw.mods.fml.common.Loader;

public final class ExternalChatCompat {
    private ExternalChatCompat() {}

    public static boolean tabbyChatLoaded() {
        return Loader.isModLoaded("tabbychat2");
    }
}
