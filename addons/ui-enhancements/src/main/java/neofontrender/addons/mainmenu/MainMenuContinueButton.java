package neofontrender.addons.mainmenu;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.Arrays;
import java.util.List;

/**
 * Shared presentation for the "continue last game" button.
 *
 * <p>Two screens can host it: the vanilla {@code GuiMainMenu} (through
 * {@code MixinGuiMainMenuContinueGame}) and Custom Main Menu's
 * {@code lumien.custommainmenu.gui.GuiCustom} (through {@link CustomMainMenuButtonInjector}). Both
 * routes build the same button, so the id, label and tooltip lines live here instead of being
 * duplicated.
 */
public final class MainMenuContinueButton extends GuiButton {

    /** Stable id lets FancyMenu cache and customize this button like other vanilla widgets. */
    public static final int BUTTON_ID = 28642;

    private final LastPlayedTarget target;

    MainMenuContinueButton(LastPlayedTarget target, int x, int y, int width, String label) {
        super(BUTTON_ID, x, y, width, 20, label);
        this.target = target;
    }

    public LastPlayedTarget target() { return target; }

    /** @return a new button, or {@code null} when the screen cannot measure it. */
    public static GuiButton create(GuiScreen screen, LastPlayedTarget target, int x, int y, int width) {
        if (screen == null || target == null) {
            return null;
        }
        FontRenderer font = screen.mc == null ? null : screen.mc.fontRenderer;
        if (font == null) {
            return null;
        }
        return new MainMenuContinueButton(target, x, y, width, fit(font, fullLabel(target), width - 8));
    }

    public static boolean isContinueButton(GuiButton button) {
        return button instanceof MainMenuContinueButton;
    }

    public static String fullLabel(LastPlayedTarget target) {
        return AddonI18n.tr("neofontrender_ui_enhancements.main_menu.continue_game")
                + ": " + target.displayName();
    }

    /** Tooltip lines describing where the button would take the player. */
    public static List<String> tooltip(LastPlayedTarget target) {
        boolean singleplayer = target.kind() == LastPlayedTarget.Kind.SINGLEPLAYER;
        String type = AddonI18n.tr(singleplayer
                ? "neofontrender_ui_enhancements.main_menu.singleplayer"
                : "neofontrender_ui_enhancements.main_menu.server");
        String detailLabel = AddonI18n.tr(singleplayer
                ? "neofontrender_ui_enhancements.main_menu.folder"
                : "neofontrender_ui_enhancements.main_menu.address");
        String detail = singleplayer ? target.identifier() : target.address();
        return Arrays.asList(type + ": " + target.displayName(), detailLabel + ": " + detail);
    }

    public static String fit(FontRenderer font, String text, int maximumWidth) {
        if (font == null || font.getStringWidth(text) <= maximumWidth) {
            return text;
        }
        String suffix = "...";
        return font.trimStringToWidth(text,
                Math.max(0, maximumWidth - font.getStringWidth(suffix))) + suffix;
    }
}
