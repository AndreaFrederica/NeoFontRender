package neofontrender.addons.tips;

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

public final class TipsSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":tips"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.tips.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1035; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = TipsConfig.enabled;
        private final int cycleTime = TipsConfig.cycleTimeMillis;
        private final boolean worldLoading = TipsConfig.showOnWorldLoading;
        private final boolean modernSplash = TipsConfig.showOnModernSplash;
        private final boolean resourceReload = TipsConfig.showOnResourceReload;
        private final boolean forgeLoading = TipsConfig.showOnForgeLoading;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.tips.enabled"), () -> tr("tooltip.tips.enabled"),
                            () -> TipsConfig.enabled, value -> TipsConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.tips.world_loading"), () -> tr("tooltip.tips.world_loading"),
                            () -> TipsConfig.showOnWorldLoading, value -> TipsConfig.showOnWorldLoading = value))
                    .add(c.toggleText(() -> tr("gui.tips.modern_splash"), () -> tr("tooltip.tips.modern_splash"),
                            () -> TipsConfig.showOnModernSplash, value -> TipsConfig.showOnModernSplash = value))
                    .add(c.toggleText(() -> tr("gui.tips.forge_loading"), () -> tr("tooltip.tips.forge_loading"),
                            () -> TipsConfig.showOnForgeLoading, value -> TipsConfig.showOnForgeLoading = value))
                    .add(c.toggleText(() -> tr("gui.tips.resource_reload"), () -> tr("tooltip.tips.resource_reload"),
                            () -> TipsConfig.showOnResourceReload, value -> TipsConfig.showOnResourceReload = value))
                    .add(c.dropdownText("tips_cycle", () -> tr("gui.tips.cycle_time"),
                            () -> Integer.toString(TipsConfig.cycleTimeMillis),
                            value -> TipsConfig.cycleTimeMillis = Integer.parseInt(value),
                            Arrays.asList("3000", "4000", "5000", "6000", "8000", "10000", "15000"),
                            value -> value + " ms").size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { TipsConfig.save(); }

        @Override public void cancel() {
            TipsConfig.enabled = enabled;
            TipsConfig.cycleTimeMillis = cycleTime;
            TipsConfig.showOnWorldLoading = worldLoading;
            TipsConfig.showOnModernSplash = modernSplash;
            TipsConfig.showOnResourceReload = resourceReload;
            TipsConfig.showOnForgeLoading = forgeLoading;
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
