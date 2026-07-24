package neofontrender.addons.hud;

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
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Settings page that previews HUD switches and themes while preserving cancel semantics. */
final class HudBarsSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":hud_bars"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.hud.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1005; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final HudBarsConfigSnapshot original = HudBarsConfigSnapshot.capture();

        @Override
        public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid()
                    .add(toggle(controls, "enabled", () -> HudBarsConfig.enabled,
                            value -> HudBarsConfig.enabled = value))
                    .add(toggle(controls, "yield_classic", () -> HudBarsConfig.yieldToClassicBar,
                            value -> HudBarsConfig.yieldToClassicBar = value))
                    .add(toggle(controls, "health", () -> HudBarsConfig.health,
                            value -> HudBarsConfig.health = value))
                    .add(toggle(controls, "absorption", () -> HudBarsConfig.absorption,
                            value -> HudBarsConfig.absorption = value))
                    .add(toggle(controls, "armor", () -> HudBarsConfig.armor,
                            value -> HudBarsConfig.armor = value))
                    .add(toggle(controls, "food", () -> HudBarsConfig.food,
                            value -> HudBarsConfig.food = value))
                    .add(toggle(controls, "air", () -> HudBarsConfig.air,
                            value -> HudBarsConfig.air = value))
                    .add(toggle(controls, "mount", () -> HudBarsConfig.mountHealth,
                            value -> HudBarsConfig.mountHealth = value))
                    .add(toggle(controls, "numbers", () -> HudBarsConfig.showNumbers,
                            value -> HudBarsConfig.showNumbers = value))
                    .add(toggle(controls, "smooth", () -> HudBarsConfig.smoothValues,
                            value -> HudBarsConfig.smoothValues = value))
                    .add(toggle(controls, "rounded", () -> HudBarsConfig.rounded,
                            value -> HudBarsConfig.rounded = value))
                    .add(controls.dropdownText(
                            "hud_bar_theme",
                            () -> tr("gui.hud.theme"),
                            () -> HudBarsConfig.theme,
                            value -> HudBarsConfig.theme = HudBarTheme.parse(value).id,
                            Arrays.asList("classic", "modern", "flat", "glass", "segmented", "minimal"),
                            value -> tr("gui.hud.theme." + value)).size(260, 24))
                    .add(controls.dropdownText(
                            "hud_bar_width",
                            () -> tr("gui.hud.width"),
                            () -> Integer.toString(HudBarsConfig.width),
                            value -> HudBarsConfig.width = Integer.parseInt(value),
                            Arrays.asList("60", "72", "81", "96", "120", "144"),
                            value -> withUnit(value, "pixels")).size(260, 24))
                    .add(controls.dropdownText(
                            "hud_bar_height",
                            () -> tr("gui.hud.height"),
                            () -> Integer.toString(HudBarsConfig.height),
                            value -> HudBarsConfig.height = Integer.parseInt(value),
                            Arrays.asList("7", "8", "9", "10", "12", "14"),
                            value -> withUnit(value, "pixels")).size(260, 24))
                    .add(controls.dropdownText(
                            "hud_bar_gap",
                            () -> tr("gui.hud.gap"),
                            () -> Integer.toString(HudBarsConfig.gap),
                            value -> HudBarsConfig.gap = Integer.parseInt(value),
                            Arrays.asList("0", "1", "2", "3", "4", "6", "8"),
                            value -> withUnit(value, "pixels")).size(260, 24))
                    .add(color(controls, "hud_background", "background",
                            () -> HudBarsConfig.background, value -> HudBarsConfig.background = value))
                    .add(color(controls, "hud_border", "border",
                            () -> HudBarsConfig.border, value -> HudBarsConfig.border = value))
                    .add(color(controls, "hud_health_low", "health_low",
                            () -> HudBarsConfig.healthColor, value -> HudBarsConfig.healthColor = value))
                    .add(color(controls, "hud_health_high", "health_high",
                            () -> HudBarsConfig.healthyColor, value -> HudBarsConfig.healthyColor = value))
                    .add(color(controls, "hud_absorption", "absorption_color",
                            () -> HudBarsConfig.absorptionColor, value -> HudBarsConfig.absorptionColor = value))
                    .add(color(controls, "hud_armor", "armor_color",
                            () -> HudBarsConfig.armorColor, value -> HudBarsConfig.armorColor = value))
                    .add(color(controls, "hud_food", "food_color",
                            () -> HudBarsConfig.foodColor, value -> HudBarsConfig.foodColor = value))
                    .add(color(controls, "hud_saturation", "saturation_color",
                            () -> HudBarsConfig.saturationColor, value -> HudBarsConfig.saturationColor = value))
                    .add(color(controls, "hud_air", "air_color",
                            () -> HudBarsConfig.airColor, value -> HudBarsConfig.airColor = value))
                    .add(color(controls, "hud_mount", "mount_color",
                            () -> HudBarsConfig.mountColor, value -> HudBarsConfig.mountColor = value));
            return new PageView(grid);
        }

        @Override
        public void apply() {
            HudBarsConfig.save();
        }

        @Override
        public void cancel() {
            original.restoreRuntime();
        }

        public void rollbackApply() {
            original.restoreAndPersist();
        }

        private static IWidget toggle(NfrSettingsControls controls, String key,
                                      Supplier<Boolean> getter, Consumer<Boolean> setter) {
            return controls.toggleText(
                    () -> tr("gui.hud." + key), () -> tr("tooltip.hud." + key), getter, setter);
        }

        private static IWidget color(NfrSettingsControls controls, String id, String key,
                                     IntSupplier getter, IntConsumer setter) {
            return controls.colorText(id, () -> tr("gui.hud." + key), getter, setter, true).size(260, 24);
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }

    private static String withUnit(String value, String unit) {
        return value + " " + tr("gui.unit." + unit);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) {
            super(section(grid, grid::preferredHeight));
        }
    }
}
