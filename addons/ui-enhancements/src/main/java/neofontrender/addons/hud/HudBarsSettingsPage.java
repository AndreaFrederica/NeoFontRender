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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class HudBarsSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":hud_bars"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.hud.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1005; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final Snapshot original = new Snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(toggle(c, "enabled", () -> HudBarsConfig.enabled, v -> HudBarsConfig.enabled = v))
                    .add(toggle(c, "yield_classic", () -> HudBarsConfig.yieldToClassicBar, v -> HudBarsConfig.yieldToClassicBar = v))
                    .add(toggle(c, "health", () -> HudBarsConfig.health, v -> HudBarsConfig.health = v))
                    .add(toggle(c, "absorption", () -> HudBarsConfig.absorption, v -> HudBarsConfig.absorption = v))
                    .add(toggle(c, "armor", () -> HudBarsConfig.armor, v -> HudBarsConfig.armor = v))
                    .add(toggle(c, "toughness", () -> HudBarsConfig.toughness, v -> HudBarsConfig.toughness = v))
                    .add(toggle(c, "food", () -> HudBarsConfig.food, v -> HudBarsConfig.food = v))
                    .add(toggle(c, "air", () -> HudBarsConfig.air, v -> HudBarsConfig.air = v))
                    .add(toggle(c, "mount", () -> HudBarsConfig.mountHealth, v -> HudBarsConfig.mountHealth = v))
                    .add(toggle(c, "numbers", () -> HudBarsConfig.showNumbers, v -> HudBarsConfig.showNumbers = v))
                    .add(toggle(c, "icons", () -> HudBarsConfig.showIcons, v -> HudBarsConfig.showIcons = v))
                    .add(toggle(c, "smooth", () -> HudBarsConfig.smoothValues, v -> HudBarsConfig.smoothValues = v))
                    .add(toggle(c, "rounded", () -> HudBarsConfig.rounded, v -> HudBarsConfig.rounded = v))
                    .add(c.dropdownText("hud_bar_theme", () -> tr("gui.hud.theme"),
                            () -> HudBarsConfig.theme, v -> HudBarsConfig.theme = HudBarTheme.parse(v).id,
                            Arrays.asList("modern", "flat", "glass", "segmented", "minimal", "classic"),
                            v -> tr("gui.hud.theme." + v)).size(260, 24))
                    .add(c.dropdownText("hud_bar_width", () -> tr("gui.hud.width"),
                            () -> Integer.toString(HudBarsConfig.width), v -> HudBarsConfig.width = Integer.parseInt(v),
                            Arrays.asList("60", "72", "81", "96", "120", "144"), v -> v + " px").size(260, 24))
                    .add(c.dropdownText("hud_bar_height", () -> tr("gui.hud.height"),
                            () -> Integer.toString(HudBarsConfig.height), v -> HudBarsConfig.height = Integer.parseInt(v),
                            Arrays.asList("7", "8", "9", "10", "12", "14"), v -> v + " px").size(260, 24))
                    .add(c.dropdownText("hud_bar_gap", () -> tr("gui.hud.gap"),
                            () -> Integer.toString(HudBarsConfig.gap), v -> HudBarsConfig.gap = Integer.parseInt(v),
                            Arrays.asList("0", "1", "2", "3", "4", "6", "8"), v -> v + " px").size(260, 24))
                    .add(c.dropdownText("hud_text_scale", () -> tr("gui.hud.text_scale"),
                            () -> Integer.toString(HudBarsConfig.textScale),
                            v -> HudBarsConfig.textScale = Integer.parseInt(v),
                            Arrays.asList("50", "60", "70", "75", "80", "90", "100", "110", "125"),
                            v -> v + "%").size(260, 24))
                    .add(c.dropdownText("hud_text_position", () -> tr("gui.hud.text_position"),
                            () -> HudBarsConfig.textPosition,
                            v -> HudBarsConfig.textPosition = HudBarsConfig.textPosition(v),
                            Arrays.asList("center", "classic"),
                            v -> tr("gui.hud.text_position." + v)).size(260, 24))
                    .add(color(c, "hud_background", "background", () -> HudBarsConfig.background, v -> HudBarsConfig.background = v))
                    .add(color(c, "hud_border", "border", () -> HudBarsConfig.border, v -> HudBarsConfig.border = v))
                    .add(color(c, "hud_health_low", "health_low", () -> HudBarsConfig.healthColor, v -> HudBarsConfig.healthColor = v))
                    .add(color(c, "hud_health_high", "health_high", () -> HudBarsConfig.healthyColor, v -> HudBarsConfig.healthyColor = v))
                    .add(color(c, "hud_absorption", "absorption_color", () -> HudBarsConfig.absorptionColor, v -> HudBarsConfig.absorptionColor = v))
                    .add(color(c, "hud_armor", "armor_color", () -> HudBarsConfig.armorColor, v -> HudBarsConfig.armorColor = v))
                    .add(color(c, "hud_toughness", "toughness_color", () -> HudBarsConfig.toughnessColor, v -> HudBarsConfig.toughnessColor = v))
                    .add(color(c, "hud_food", "food_color", () -> HudBarsConfig.foodColor, v -> HudBarsConfig.foodColor = v))
                    .add(color(c, "hud_saturation", "saturation_color", () -> HudBarsConfig.saturationColor, v -> HudBarsConfig.saturationColor = v))
                    .add(color(c, "hud_air", "air_color", () -> HudBarsConfig.airColor, v -> HudBarsConfig.airColor = v))
                    .add(color(c, "hud_mount", "mount_color", () -> HudBarsConfig.mountColor, v -> HudBarsConfig.mountColor = v));
            return new PageView(grid);
        }

        @Override public void apply() { HudBarsConfig.save(); }
        @Override public void cancel() { original.restore(); }

        private static IWidget toggle(NfrSettingsControls c, String key,
                                      java.util.function.Supplier<Boolean> get,
                                      java.util.function.Consumer<Boolean> set) {
            return c.toggleText(() -> tr("gui.hud." + key), () -> tr("tooltip.hud." + key), get, set);
        }

        private static IWidget color(NfrSettingsControls c, String id, String key,
                                     IntSupplier get, IntConsumer set) {
            return c.colorText(id, () -> tr("gui.hud." + key), get, set, true).size(260, 24);
        }
    }

    private static final class Snapshot {
        private final boolean[] b = { HudBarsConfig.enabled, HudBarsConfig.yieldToClassicBar,
                HudBarsConfig.health, HudBarsConfig.absorption, HudBarsConfig.armor, HudBarsConfig.toughness,
                HudBarsConfig.food, HudBarsConfig.air, HudBarsConfig.mountHealth, HudBarsConfig.showNumbers,
                HudBarsConfig.showIcons, HudBarsConfig.smoothValues, HudBarsConfig.rounded };
        private final int width = HudBarsConfig.width, height = HudBarsConfig.height, gap = HudBarsConfig.gap;
        private final int textScale = HudBarsConfig.textScale;
        private final String textPosition = HudBarsConfig.textPosition;
        private final String theme = HudBarsConfig.theme;
        private final int[] c = { HudBarsConfig.background, HudBarsConfig.border, HudBarsConfig.healthColor,
                HudBarsConfig.healthyColor, HudBarsConfig.absorptionColor, HudBarsConfig.armorColor,
                HudBarsConfig.toughnessColor, HudBarsConfig.foodColor, HudBarsConfig.saturationColor,
                HudBarsConfig.airColor, HudBarsConfig.mountColor };

        private void restore() {
            HudBarsConfig.enabled=b[0]; HudBarsConfig.yieldToClassicBar=b[1]; HudBarsConfig.health=b[2];
            HudBarsConfig.absorption=b[3]; HudBarsConfig.armor=b[4]; HudBarsConfig.toughness=b[5];
            HudBarsConfig.food=b[6]; HudBarsConfig.air=b[7]; HudBarsConfig.mountHealth=b[8];
            HudBarsConfig.showNumbers=b[9]; HudBarsConfig.showIcons=b[10];
            HudBarsConfig.smoothValues=b[11]; HudBarsConfig.rounded=b[12];
            HudBarsConfig.theme=theme;
            HudBarsConfig.width=width; HudBarsConfig.height=height; HudBarsConfig.gap=gap;
            HudBarsConfig.textScale=textScale;
            HudBarsConfig.textPosition=textPosition;
            HudBarsConfig.background=c[0]; HudBarsConfig.border=c[1]; HudBarsConfig.healthColor=c[2];
            HudBarsConfig.healthyColor=c[3]; HudBarsConfig.absorptionColor=c[4]; HudBarsConfig.armorColor=c[5];
            HudBarsConfig.toughnessColor=c[6]; HudBarsConfig.foodColor=c[7]; HudBarsConfig.saturationColor=c[8];
            HudBarsConfig.airColor=c[9]; HudBarsConfig.mountColor=c[10];
        }
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
