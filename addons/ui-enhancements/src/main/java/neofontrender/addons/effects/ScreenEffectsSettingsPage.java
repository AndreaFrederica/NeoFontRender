package neofontrender.addons.effects;

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

final class ScreenEffectsSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":screen_effects"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.effects.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1030; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = ScreenEffectsConfig.enabled;
        private final boolean fade = ScreenEffectsConfig.fade;
        private final int duration = ScreenEffectsConfig.fadeDurationMillis;
        private final boolean blur = ScreenEffectsConfig.blur;
        private final float radius = ScreenEffectsConfig.blurRadius;
        private final boolean gradient = ScreenEffectsConfig.gradient;
        private final int[] colors = ScreenEffectsConfig.colors.clone();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.effects.enabled"), () -> tr("tooltip.effects.enabled"),
                            () -> ScreenEffectsConfig.enabled, value -> ScreenEffectsConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.effects.fade"), () -> "",
                            () -> ScreenEffectsConfig.fade, value -> ScreenEffectsConfig.fade = value))
                    .add(c.dropdownText("effects_fade_duration", () -> tr("gui.effects.fade_duration"),
                            () -> Integer.toString(ScreenEffectsConfig.fadeDurationMillis),
                            value -> ScreenEffectsConfig.fadeDurationMillis = Integer.parseInt(value),
                            Arrays.asList("0", "100", "160", "220", "300", "450", "700", "1000"),
                            value -> value + " ms").size(260, 24))
                    .add(c.toggleText(() -> tr("gui.effects.blur"), () -> tr("tooltip.effects.blur"),
                            () -> ScreenEffectsConfig.blur, value -> ScreenEffectsConfig.blur = value))
                    .add(c.dropdownText("effects_blur_radius", () -> tr("gui.effects.blur_radius"),
                            () -> number(ScreenEffectsConfig.blurRadius),
                            value -> ScreenEffectsConfig.blurRadius = Float.parseFloat(value),
                            Arrays.asList("1", "2", "3", "4", "5", "7", "9", "12", "16"), value -> value).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.effects.gradient"), () -> "",
                            () -> ScreenEffectsConfig.gradient, value -> ScreenEffectsConfig.gradient = value));
            String[] corners = {"ul", "ur", "lr", "ll"};
            for (int i = 0; i < 4; i++) {
                final int corner = i;
                grid.add(c.colorText("effects_color_" + corners[i],
                        () -> tr("gui.effects.color") + " · " + tr("gui.corner." + corners[corner]),
                        () -> ScreenEffectsConfig.colors[corner], value -> ScreenEffectsConfig.colors[corner] = value, true)
                        .size(260, 24));
            }
            return new PageView(grid);
        }

        @Override public void apply() { ScreenEffectsConfig.save(); }
        @Override public void cancel() {
            ScreenEffectsConfig.enabled = enabled;
            ScreenEffectsConfig.fade = fade;
            ScreenEffectsConfig.fadeDurationMillis = duration;
            ScreenEffectsConfig.blur = blur;
            ScreenEffectsConfig.blurRadius = radius;
            ScreenEffectsConfig.gradient = gradient;
            ScreenEffectsConfig.colors = colors.clone();
        }
    }

    private static String number(float value) { return value == (int) value ? Integer.toString((int) value) : Float.toString(value); }
    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
