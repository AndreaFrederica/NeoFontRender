package neofontrender.addons.hover;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class HoverEffectsSettingsPage implements NfrSettingsPage {
    private static final List<String> DURATIONS =
            Arrays.asList("0", "60", "80", "100", "120", "160", "180", "220", "300", "500");

    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":hover"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.hover.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1018; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = HoverEffectsConfig.enabled;
        private final boolean buttons = HoverEffectsConfig.buttons;
        private final int buttonEnter = HoverEffectsConfig.buttonEnterMillis;
        private final int buttonExit = HoverEffectsConfig.buttonExitMillis;
        private final boolean slots = HoverEffectsConfig.slots;
        private final int slotEnter = HoverEffectsConfig.slotEnterMillis;
        private final int slotExit = HoverEffectsConfig.slotExitMillis;
        private final int slotColor = HoverEffectsConfig.slotColor;
        private final boolean jeiIngredientGrid = HoverEffectsConfig.jeiIngredientGrid;
        private final boolean modularUiSlots = HoverEffectsConfig.modularUiSlots;
        private final boolean modularUiThemeColor = HoverEffectsConfig.modularUiThemeColor;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.hover.enabled"), () -> tr("tooltip.hover.enabled"),
                            () -> HoverEffectsConfig.enabled, value -> HoverEffectsConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.hover.buttons"), () -> tr("tooltip.hover.buttons"),
                            () -> HoverEffectsConfig.buttons, value -> HoverEffectsConfig.buttons = value))
                    .add(duration(c, "hover_button_enter", "gui.hover.enter", () -> HoverEffectsConfig.buttonEnterMillis,
                            value -> HoverEffectsConfig.buttonEnterMillis = value))
                    .add(duration(c, "hover_button_exit", "gui.hover.exit", () -> HoverEffectsConfig.buttonExitMillis,
                            value -> HoverEffectsConfig.buttonExitMillis = value))
                    .add(c.toggleText(() -> tr("gui.hover.slots"), () -> tr("tooltip.hover.slots"),
                            () -> HoverEffectsConfig.slots, value -> HoverEffectsConfig.slots = value))
                    .add(duration(c, "hover_slot_enter", "gui.hover.slot_enter", () -> HoverEffectsConfig.slotEnterMillis,
                            value -> HoverEffectsConfig.slotEnterMillis = value))
                    .add(duration(c, "hover_slot_exit", "gui.hover.slot_exit", () -> HoverEffectsConfig.slotExitMillis,
                            value -> HoverEffectsConfig.slotExitMillis = value))
                    .add(c.colorText("hover_slot_color", () -> tr("gui.hover.slot_color"),
                            () -> HoverEffectsConfig.slotColor, value -> HoverEffectsConfig.slotColor = value, true).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.hover.jei_ingredient_grid"),
                            () -> tr("tooltip.hover.jei_ingredient_grid"),
                            () -> HoverEffectsConfig.jeiIngredientGrid,
                            value -> HoverEffectsConfig.jeiIngredientGrid = value))
                    .add(c.toggleText(() -> tr("gui.hover.modularui_slots"), () -> tr("tooltip.hover.modularui_slots"),
                            () -> HoverEffectsConfig.modularUiSlots, value -> HoverEffectsConfig.modularUiSlots = value))
                    .add(c.toggleText(() -> tr("gui.hover.modularui_theme_color"),
                            () -> tr("tooltip.hover.modularui_theme_color"),
                            () -> HoverEffectsConfig.modularUiThemeColor,
                            value -> HoverEffectsConfig.modularUiThemeColor = value));
            return new PageView(grid);
        }

        @Override public void apply() { HoverEffectsConfig.save(); }

        @Override public void cancel() {
            HoverEffectsConfig.enabled = enabled;
            HoverEffectsConfig.buttons = buttons;
            HoverEffectsConfig.buttonEnterMillis = buttonEnter;
            HoverEffectsConfig.buttonExitMillis = buttonExit;
            HoverEffectsConfig.slots = slots;
            HoverEffectsConfig.slotEnterMillis = slotEnter;
            HoverEffectsConfig.slotExitMillis = slotExit;
            HoverEffectsConfig.slotColor = slotColor;
            HoverEffectsConfig.jeiIngredientGrid = jeiIngredientGrid;
            HoverEffectsConfig.modularUiSlots = modularUiSlots;
            HoverEffectsConfig.modularUiThemeColor = modularUiThemeColor;
        }

        private static IWidget duration(NfrSettingsControls controls, String name, String label,
                                        IntSupplier getter, IntConsumer setter) {
            return controls.dropdownText(name, () -> tr(label), () -> Integer.toString(getter.getAsInt()),
                    value -> setter.accept(Integer.parseInt(value)), DURATIONS,
                    value -> value.equals("0") ? tr("gui.hover.instant") : value + " ms").size(260, 24);
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
