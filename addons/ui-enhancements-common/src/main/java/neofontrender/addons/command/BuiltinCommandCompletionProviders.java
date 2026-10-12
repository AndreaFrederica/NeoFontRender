package neofontrender.addons.command;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.command.server.ServerCommandCompletionApi;
import neofontrender.addons.compat.thaumcraft.ThaumcraftCommandCompletionProvider;

/** Registers built-in adapters without exposing optional-mod classes through the public API. */
public final class BuiltinCommandCompletionProviders {
    private static boolean initialized;

    private BuiltinCommandCompletionProviders() {}

    public static synchronized void ensureRegistered() {
        if (initialized) return;
        initialized = true;
        ServerCommandCompletionApi.registerProvider(
                new ResourceLocation("neofontrender_ui_enhancements", "thaumcraft_6"),
                0, new ThaumcraftCommandCompletionProvider());
    }
}
