package neofontrender.addons.scrolling;

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

final class SmoothScrollingSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":scrolling"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.scrolling.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1010; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalEnabled = SmoothScrollConfig.enabled;
        private final boolean originalVanillaLists = SmoothScrollConfig.vanillaLists;
        private final boolean originalForgeLists = SmoothScrollConfig.forgeLists;
        private final boolean originalCreativeInventory = SmoothScrollConfig.creativeInventory;
        private final boolean originalChat = SmoothScrollConfig.chat;
        private final int originalDuration = SmoothScrollConfig.durationMillis;
        private final int originalStep = SmoothScrollConfig.wheelStep;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.scrolling.enabled"), () -> tr("tooltip.scrolling.enabled"),
                            () -> SmoothScrollConfig.enabled, value -> SmoothScrollConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.scrolling.vanilla_lists"), () -> tr("tooltip.scrolling.vanilla_lists"),
                            () -> SmoothScrollConfig.vanillaLists, value -> SmoothScrollConfig.vanillaLists = value))
                    .add(c.toggleText(() -> tr("gui.scrolling.forge_lists"), () -> tr("tooltip.scrolling.forge_lists"),
                            () -> SmoothScrollConfig.forgeLists, value -> SmoothScrollConfig.forgeLists = value))
                    .add(c.toggleText(() -> tr("gui.scrolling.creative_inventory"), () -> tr("tooltip.scrolling.creative_inventory"),
                            () -> SmoothScrollConfig.creativeInventory, value -> SmoothScrollConfig.creativeInventory = value))
                    .add(c.toggleText(() -> tr("gui.scrolling.chat"), () -> tr("tooltip.scrolling.chat"),
                            () -> SmoothScrollConfig.chat, value -> SmoothScrollConfig.chat = value))
                    .add(c.dropdownText("scroll_duration", () -> tr("gui.scrolling.duration"),
                            () -> Integer.toString(SmoothScrollConfig.durationMillis),
                            value -> SmoothScrollConfig.durationMillis = Integer.parseInt(value),
                            Arrays.asList("80", "120", "160", "200", "260", "350", "500"),
                            value -> value + " ms").size(260, 24))
                    .add(c.dropdownText("scroll_step", () -> tr("gui.scrolling.step"),
                            () -> Integer.toString(SmoothScrollConfig.wheelStep),
                            value -> SmoothScrollConfig.wheelStep = Integer.parseInt(value),
                            Arrays.asList("16", "24", "32", "40", "56", "80", "120"),
                            value -> value + " px").size(260, 24));
            return new PageView(grid);
        }

        @Override public void apply() { SmoothScrollConfig.save(); }
        @Override public void cancel() {
            SmoothScrollConfig.enabled = originalEnabled;
            SmoothScrollConfig.vanillaLists = originalVanillaLists;
            SmoothScrollConfig.forgeLists = originalForgeLists;
            SmoothScrollConfig.creativeInventory = originalCreativeInventory;
            SmoothScrollConfig.chat = originalChat;
            SmoothScrollConfig.durationMillis = originalDuration;
            SmoothScrollConfig.wheelStep = originalStep;
        }
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
