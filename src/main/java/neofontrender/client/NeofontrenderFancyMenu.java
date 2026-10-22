package neofontrender.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.Loader;
import neofontrender.core.config.NeofontrenderConfig;

/** Optional FancyMenu 2.x (Minecraft 1.12.2) bridge. No optional types in signatures. */
public final class NeofontrenderFancyMenu {
    public static final String MOD_ID = "fancymenu";
    private static final String CUSTOMIZATION =
            "de.keksuccino.fancymenu.menu.fancy.MenuCustomization";

    private NeofontrenderFancyMenu() {}

    public static boolean present() {
        return Loader.isModLoaded(MOD_ID);
    }

    public static boolean brandOverrideActive() {
        return !present() || NeofontrenderConfig.compatFancyMenu();
    }

    /** FancyMenu customizes GuiMainMenu in place; its editor and custom subpages are excluded. */
    public static boolean ownsScreen(GuiScreen screen) {
        if (screen == null || screen.getClass() != GuiMainMenu.class || !present()) return false;
        try {
            return Boolean.TRUE.equals(Class.forName(CUSTOMIZATION)
                    .getMethod("isMenuCustomizable", GuiScreen.class).invoke(null, screen));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    /** Hidden buttons can remain in the vanilla list; do not draw tooltips for them. */
    public static boolean isButtonHidden(GuiScreen screen, GuiButton button) {
        try {
            Object handler = Class.forName("de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerRegistry")
                    .getMethod("getHandlerFor", GuiScreen.class).invoke(null, screen);
            return handler != null && Boolean.TRUE.equals(handler.getClass()
                    .getMethod("isVanillaButtonHidden", GuiButton.class).invoke(handler, button));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }
}
