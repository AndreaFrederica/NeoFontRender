package neofontrender.addons.flight;

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

final class CrosshairSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":crosshair"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.crosshair.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1017; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = CrosshairConfig.customEnabled;
        private final boolean hideVanillaFlight = CrosshairConfig.hideVanillaDuringFlightHud;
        private final boolean hideForgeFlight = CrosshairConfig.hideForgeLayerDuringFlightHud;
        private final String style = CrosshairConfig.style;
        private final int color = CrosshairConfig.color;
        private final int scale = CrosshairConfig.scalePercent;
        private final int gap = CrosshairConfig.gap;
        private final int arm = CrosshairConfig.armLength;
        private final int thickness = CrosshairConfig.thickness;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.crosshair.enabled"),
                            () -> tr("tooltip.crosshair.enabled"),
                            () -> CrosshairConfig.customEnabled,
                            value -> CrosshairConfig.customEnabled = value))
                    .add(c.toggleText(() -> tr("gui.crosshair.hide_flight"),
                            () -> tr("tooltip.crosshair.hide_flight"),
                            () -> CrosshairConfig.hideVanillaDuringFlightHud,
                            value -> CrosshairConfig.hideVanillaDuringFlightHud = value))
                    .add(c.toggleText(() -> tr("gui.crosshair.hide_forge_flight"),
                            () -> tr("tooltip.crosshair.hide_forge_flight"),
                            () -> CrosshairConfig.hideForgeLayerDuringFlightHud,
                            value -> CrosshairConfig.hideForgeLayerDuringFlightHud = value))
                    .add(c.dropdownText("crosshair_style", () -> tr("gui.crosshair.style"),
                            () -> CrosshairConfig.style, value -> CrosshairConfig.style = value,
                            Arrays.asList("cross", "dot", "circle", "chevron"),
                            value -> tr("gui.crosshair.style." + value)).size(260, 24))
                    .add(c.colorText("crosshair_color", () -> tr("gui.crosshair.color"),
                            () -> CrosshairConfig.color, value -> CrosshairConfig.color = value, true)
                            .size(260, 24))
                    .add(c.decimalSlider("neofontrender_ui_enhancements.gui.crosshair.scale",
                            () -> (float) CrosshairConfig.scalePercent,
                            value -> CrosshairConfig.scalePercent = Math.round(value), 50, 300, 1))
                    .add(c.decimalSlider("neofontrender_ui_enhancements.gui.crosshair.gap",
                            () -> (float) CrosshairConfig.gap,
                            value -> CrosshairConfig.gap = Math.round(value), 0, 16, 1))
                    .add(c.decimalSlider("neofontrender_ui_enhancements.gui.crosshair.arm",
                            () -> (float) CrosshairConfig.armLength,
                            value -> CrosshairConfig.armLength = Math.round(value), 1, 24, 1))
                    .add(c.decimalSlider("neofontrender_ui_enhancements.gui.crosshair.thickness",
                            () -> (float) CrosshairConfig.thickness,
                            value -> CrosshairConfig.thickness = Math.round(value), 1, 6, 1));
            return new PageView(grid);
        }

        @Override public void apply() { CrosshairConfig.save(); }

        @Override public void cancel() {
            CrosshairConfig.customEnabled = enabled;
            CrosshairConfig.hideVanillaDuringFlightHud = hideVanillaFlight;
            CrosshairConfig.hideForgeLayerDuringFlightHud = hideForgeFlight;
            CrosshairConfig.style = style;
            CrosshairConfig.color = color;
            CrosshairConfig.scalePercent = scale;
            CrosshairConfig.gap = gap;
            CrosshairConfig.armLength = arm;
            CrosshairConfig.thickness = thickness;
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
