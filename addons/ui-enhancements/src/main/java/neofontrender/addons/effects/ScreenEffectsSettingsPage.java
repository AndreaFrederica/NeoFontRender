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
        private final boolean fadeMenus = ScreenEffectsConfig.fadeMenus;
        private final boolean fadeContainers = ScreenEffectsConfig.fadeContainers;
        private final boolean fadeChat = ScreenEffectsConfig.fadeChat;
        private final boolean blur = ScreenEffectsConfig.blur;
        private final boolean blurMenus = ScreenEffectsConfig.blurMenus;
        private final boolean blurContainers = ScreenEffectsConfig.blurContainers;
        private final boolean blurChat = ScreenEffectsConfig.blurChat;
        private final int radius = ScreenEffectsConfig.blurRadius;
        private final boolean gradient = ScreenEffectsConfig.gradient;
        private final boolean gradientMenus = ScreenEffectsConfig.gradientMenus;
        private final boolean gradientContainers = ScreenEffectsConfig.gradientContainers;
        private final boolean gradientChat = ScreenEffectsConfig.gradientChat;
        private final int[] colors = ScreenEffectsConfig.colors.clone();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.effects.enabled"), () -> tr("tooltip.effects.enabled"),
                            () -> ScreenEffectsConfig.enabled, value -> ScreenEffectsConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.effects.fade"), () -> "",
                            () -> ScreenEffectsConfig.fade, value -> ScreenEffectsConfig.fade = value))
                    .add(c.toggleText(() -> scoped("gui.effects.fade", "gui.effects.scope.menus"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.fadeMenus, value -> ScreenEffectsConfig.fadeMenus = value))
                    .add(c.toggleText(() -> scoped("gui.effects.fade", "gui.effects.scope.containers"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.fadeContainers, value -> ScreenEffectsConfig.fadeContainers = value))
                    .add(c.toggleText(() -> scoped("gui.effects.fade", "gui.effects.scope.chat"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.fadeChat, value -> ScreenEffectsConfig.fadeChat = value))
                    .add(c.dropdownText("effects_fade_duration", () -> tr("gui.effects.fade_duration"),
                            () -> Integer.toString(ScreenEffectsConfig.fadeDurationMillis),
                            value -> ScreenEffectsConfig.fadeDurationMillis = Integer.parseInt(value),
                            Arrays.asList("0", "100", "160", "220", "300", "450", "700", "1000"),
                            value -> value + " ms").size(260, 24))
                    .add(c.toggleText(() -> tr("gui.effects.blur"), () -> tr("tooltip.effects.blur"),
                            () -> ScreenEffectsConfig.blur, value -> ScreenEffectsConfig.blur = value))
                    .add(c.toggleText(() -> scoped("gui.effects.blur", "gui.effects.scope.menus"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.blurMenus, value -> ScreenEffectsConfig.blurMenus = value))
                    .add(c.toggleText(() -> scoped("gui.effects.blur", "gui.effects.scope.containers"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.blurContainers, value -> ScreenEffectsConfig.blurContainers = value))
                    .add(c.toggleText(() -> scoped("gui.effects.blur", "gui.effects.scope.chat"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.blurChat, value -> ScreenEffectsConfig.blurChat = value))
                    .add(c.dropdownText("effects_blur_radius", () -> tr("gui.effects.blur_radius"),
                            () -> Integer.toString(ScreenEffectsConfig.blurRadius),
                            value -> ScreenEffectsConfig.blurRadius = Integer.parseInt(value),
                            Arrays.asList("1", "2", "3", "4", "5", "7", "9", "12", "16"), value -> value).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.effects.gradient"), () -> "",
                            () -> ScreenEffectsConfig.gradient, value -> ScreenEffectsConfig.gradient = value))
                    .add(c.toggleText(() -> scoped("gui.effects.gradient", "gui.effects.scope.menus"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.gradientMenus, value -> ScreenEffectsConfig.gradientMenus = value))
                    .add(c.toggleText(() -> scoped("gui.effects.gradient", "gui.effects.scope.containers"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.gradientContainers, value -> ScreenEffectsConfig.gradientContainers = value))
                    .add(c.toggleText(() -> scoped("gui.effects.gradient", "gui.effects.scope.chat"), () -> tr("tooltip.effects.scope"),
                            () -> ScreenEffectsConfig.gradientChat, value -> ScreenEffectsConfig.gradientChat = value));
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
            ScreenEffectsConfig.fadeMenus = fadeMenus;
            ScreenEffectsConfig.fadeContainers = fadeContainers;
            ScreenEffectsConfig.fadeChat = fadeChat;
            ScreenEffectsConfig.blur = blur;
            ScreenEffectsConfig.blurMenus = blurMenus;
            ScreenEffectsConfig.blurContainers = blurContainers;
            ScreenEffectsConfig.blurChat = blurChat;
            ScreenEffectsConfig.blurRadius = radius;
            ScreenEffectsConfig.gradient = gradient;
            ScreenEffectsConfig.gradientMenus = gradientMenus;
            ScreenEffectsConfig.gradientContainers = gradientContainers;
            ScreenEffectsConfig.gradientChat = gradientChat;
            ScreenEffectsConfig.colors = colors.clone();
        }
    }

    private static String scoped(String effectKey, String scopeKey) {
        return tr(effectKey) + " · " + tr(scopeKey);
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
