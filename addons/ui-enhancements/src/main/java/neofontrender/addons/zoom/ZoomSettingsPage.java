package neofontrender.addons.zoom;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrDecimalSlider;
import neofontrender.client.gui.component.base.NfrDoubleValue;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;
import java.util.Locale;

final class ZoomSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":zoom"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.zoom.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1015; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalEnabled = ZoomConfig.enabled;
        private final float originalMagnification = ZoomConfig.magnification;
        private final boolean originalSmoothCamera = ZoomConfig.smoothCamera;
        private final int originalMouseSensitivityAdjustment =
                ZoomConfig.mouseSensitivityAdjustmentPercent;
        private final boolean originalSmoothTransition = ZoomConfig.smoothTransition;
        private final int originalTransitionDuration = ZoomConfig.transitionDurationMillis;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid()
                    .add(controls.toggleText(() -> tr("gui.zoom.enabled"), () -> tr("tooltip.zoom.enabled"),
                            () -> ZoomConfig.enabled, value -> ZoomConfig.enabled = value))
                    .add(controls.dropdownText("zoom_magnification", () -> tr("gui.zoom.magnification"),
                            () -> String.format(Locale.ROOT, "%.1f", ZoomConfig.magnification),
                            value -> ZoomConfig.magnification = Float.parseFloat(value),
                            Arrays.asList("2.0", "3.0", "4.0", "5.0", "6.0", "8.0"),
                            ZoomSettingsPage::magnificationLabel).size(260, 24))
                    .add(controls.toggleText(() -> tr("gui.zoom.smooth_camera"),
                            () -> tr("tooltip.zoom.smooth_camera"),
                            () -> ZoomConfig.smoothCamera, value -> ZoomConfig.smoothCamera = value))
                    .add(mouseSensitivitySlider())
                    .add(controls.toggleText(() -> tr("gui.zoom.smooth_transition"),
                            () -> tr("tooltip.zoom.smooth_transition"),
                            () -> ZoomConfig.smoothTransition, value -> ZoomConfig.smoothTransition = value))
                    .add(controls.dropdownText("zoom_transition_duration",
                            () -> tr("gui.zoom.transition_duration"),
                            () -> Integer.toString(ZoomConfig.transitionDurationMillis),
                            value -> ZoomConfig.transitionDurationMillis = Integer.parseInt(value),
                            Arrays.asList("80", "120", "160", "200", "260", "350", "500"),
                            value -> value + " ms").size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { ZoomConfig.save(); }

        @Override public void cancel() {
            ZoomConfig.enabled = originalEnabled;
            ZoomConfig.magnification = originalMagnification;
            ZoomConfig.smoothCamera = originalSmoothCamera;
            ZoomConfig.mouseSensitivityAdjustmentPercent = originalMouseSensitivityAdjustment;
            ZoomConfig.smoothTransition = originalSmoothTransition;
            ZoomConfig.transitionDurationMillis = originalTransitionDuration;
        }
    }

    private static String magnificationLabel(String value) {
        float parsed = Float.parseFloat(value);
        return (parsed == Math.round(parsed) ? Integer.toString(Math.round(parsed)) : value) + "x";
    }

    private static IWidget mouseSensitivitySlider() {
        NfrDecimalSlider slider = new NfrDecimalSlider(
                () -> tr("gui.zoom.mouse_sensitivity"),
                ZoomSettingsPage::mouseSensitivityAdjustmentLabel);
        slider.value(new NfrDoubleValue(
                () -> (double) ZoomConfig.mouseSensitivityAdjustmentPercent,
                value -> ZoomConfig.mouseSensitivityAdjustmentPercent = Math.max(-100,
                        Math.min(100, (int) Math.round(value / 5.0D) * 5))));
        slider.bounds(-100.0D, 100.0D);
        slider.tooltip(new RichTooltip().showUpTimer(8)
                .addLine(tr("tooltip.zoom.mouse_sensitivity")));
        return slider.size(260, 24);
    }

    private static String mouseSensitivityAdjustmentLabel() {
        int value = ZoomConfig.mouseSensitivityAdjustmentPercent;
        return (value > 0 ? "+" : "") + value + "%";
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
