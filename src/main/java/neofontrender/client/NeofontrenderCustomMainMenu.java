package neofontrender.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.Loader;
import neofontrender.core.config.NeofontrenderConfig;

/**
 * Optional CMM bridge. Init events expose GuiFakeMain and a temporary button list, whereas
 * rendering uses GuiCustom. The guiConfig name distinguishes the title screen from subpages.
 * Nova branding is a global installation policy; screen labels use the actual screen identity.
 */
public final class NeofontrenderCustomMainMenu {

    /** Mod id declared by CMM's {@code mcmod.info}. */
    public static final String MOD_ID = "custommainmenu";

    /** CMM's custom screen. Matched by name because NFR must not hard-depend on CMM. */
    private static final String CUSTOM_SCREEN_CLASS = "lumien.custommainmenu.gui.GuiCustom";

    private NeofontrenderCustomMainMenu() {}

    /** @return true when Custom Main Menu is installed, regardless of whether it owns the menu. */
    public static boolean present() {
        // Runtime query only; mixin selection must not call Forge before mod discovery.
        return Loader.isModLoaded(MOD_ID);
    }

    /** @return true when the given screen is CMM's custom screen rather than a vanilla title screen. */
    public static boolean ownsScreen(GuiScreen screen) {
        if (screen == null || !present()) {
            return false;
        }
        return isTitleScreen(screen);
    }

    static boolean isTitleScreen(Object screen) {
        if (screen == null) return false;
        if (!CUSTOM_SCREEN_CLASS.equals(screen.getClass().getName())) return false;
        try {
            Object config = screen.getClass().getField("guiConfig").get(screen);
            return config != null && "mainmenu".equals(config.getClass().getField("name").get(config));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * @return whether CMM permits the global nova easter egg. Installation, not the current
     *         screen, controls this policy because mod names and key categories initialize early.
     */
    public static boolean brandOverrideActive() {
        if (!present() || NeofontrenderConfig.compatCustomMainMenu()) {
            return true;
        }
        // Global names and key categories are resolved before a title screen exists.
        return false;
    }

}
