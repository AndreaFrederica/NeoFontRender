package neofontrender.addons.loading;

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

final class WorldLoadingSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":world_loading"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.loading.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1040; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = WorldLoadingConfig.enabled;
        private final boolean worldJoin = WorldLoadingConfig.worldJoin;
        private final boolean dimensionChange = WorldLoadingConfig.dimensionChange;
        private final boolean singleplayerProgress = WorldLoadingConfig.singleplayerServerProgress;
        private final boolean lastExitSnapshot = WorldLoadingConfig.lastExitSnapshot;
        private final boolean bottomShade = WorldLoadingConfig.bottomShade;
        private final boolean progressBar = WorldLoadingConfig.progressBar;
        private final boolean percentage = WorldLoadingConfig.percentage;
        private final boolean spinner = WorldLoadingConfig.spinner;
        private final boolean fadeOut = WorldLoadingConfig.fadeOut;
        private final int fadeDuration = WorldLoadingConfig.fadeOutDurationMillis;
        private final int accentColor = WorldLoadingConfig.accentColor;
        private final int textColor = WorldLoadingConfig.textColor;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.loading.enabled"), () -> tr("tooltip.loading.enabled"),
                            () -> WorldLoadingConfig.enabled, value -> WorldLoadingConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.loading.world_join"), () -> tr("tooltip.loading.scope"),
                            () -> WorldLoadingConfig.worldJoin, value -> WorldLoadingConfig.worldJoin = value))
                    .add(c.toggleText(() -> tr("gui.loading.dimension_change"), () -> tr("tooltip.loading.scope"),
                            () -> WorldLoadingConfig.dimensionChange, value -> WorldLoadingConfig.dimensionChange = value))
                    .add(c.toggleText(() -> tr("gui.loading.singleplayer_progress"),
                            () -> tr("tooltip.loading.singleplayer_progress"),
                            () -> WorldLoadingConfig.singleplayerServerProgress,
                            value -> WorldLoadingConfig.singleplayerServerProgress = value))
                    .add(c.toggleText(() -> tr("gui.loading.last_exit_snapshot"),
                            () -> tr("tooltip.loading.last_exit_snapshot"),
                            () -> WorldLoadingConfig.lastExitSnapshot,
                            value -> WorldLoadingConfig.lastExitSnapshot = value))
                    .add(c.toggleText(() -> tr("gui.loading.bottom_shade"), () -> "",
                            () -> WorldLoadingConfig.bottomShade, value -> WorldLoadingConfig.bottomShade = value))
                    .add(c.toggleText(() -> tr("gui.loading.progress_bar"), () -> "",
                            () -> WorldLoadingConfig.progressBar, value -> WorldLoadingConfig.progressBar = value))
                    .add(c.toggleText(() -> tr("gui.loading.percentage"), () -> tr("tooltip.loading.percentage"),
                            () -> WorldLoadingConfig.percentage, value -> WorldLoadingConfig.percentage = value))
                    .add(c.toggleText(() -> tr("gui.loading.spinner"), () -> "",
                            () -> WorldLoadingConfig.spinner, value -> WorldLoadingConfig.spinner = value))
                    .add(c.toggleText(() -> tr("gui.loading.fade_out"), () -> "",
                            () -> WorldLoadingConfig.fadeOut, value -> WorldLoadingConfig.fadeOut = value))
                    .add(c.dropdownText("loading_fade_duration", () -> tr("gui.loading.fade_duration"),
                            () -> Integer.toString(WorldLoadingConfig.fadeOutDurationMillis),
                            value -> WorldLoadingConfig.fadeOutDurationMillis = Integer.parseInt(value),
                            Arrays.asList("0", "120", "220", "360", "500", "750", "1000", "1500"),
                            value -> value + " ms").size(260, 24))
                    .add(c.colorText("loading_accent", () -> tr("gui.loading.accent_color"),
                            () -> WorldLoadingConfig.accentColor,
                            value -> WorldLoadingConfig.accentColor = value, true).size(260, 24))
                    .add(c.colorText("loading_text", () -> tr("gui.loading.text_color"),
                            () -> WorldLoadingConfig.textColor,
                            value -> WorldLoadingConfig.textColor = value, true).size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { WorldLoadingConfig.save(); }

        @Override public void cancel() {
            WorldLoadingConfig.enabled = enabled;
            WorldLoadingConfig.worldJoin = worldJoin;
            WorldLoadingConfig.dimensionChange = dimensionChange;
            WorldLoadingConfig.singleplayerServerProgress = singleplayerProgress;
            WorldLoadingConfig.lastExitSnapshot = lastExitSnapshot;
            WorldLoadingConfig.bottomShade = bottomShade;
            WorldLoadingConfig.progressBar = progressBar;
            WorldLoadingConfig.percentage = percentage;
            WorldLoadingConfig.spinner = spinner;
            WorldLoadingConfig.fadeOut = fadeOut;
            WorldLoadingConfig.fadeOutDurationMillis = fadeDuration;
            WorldLoadingConfig.accentColor = accentColor;
            WorldLoadingConfig.textColor = textColor;
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
