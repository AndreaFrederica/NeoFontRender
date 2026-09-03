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

/** Cross-feature UIE options that do not belong to a dedicated feature page. */
final class MiscSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":misc"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.misc.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1018; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalGround = CrosshairConfig.cancelCrosshairEventOnGround;
        private final boolean originalFlight = CrosshairConfig.cancelCrosshairEventDuringFlight;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid();
            grid.add(controls.toggleText(
                    () -> tr("gui.misc.cancel_crosshair_ground"),
                    () -> tr("tooltip.misc.cancel_crosshair_ground"),
                    () -> CrosshairConfig.cancelCrosshairEventOnGround,
                    value -> CrosshairConfig.cancelCrosshairEventOnGround = value));
            grid.add(controls.toggleText(
                    () -> tr("gui.misc.cancel_crosshair_flight"),
                    () -> tr("tooltip.misc.cancel_crosshair_flight"),
                    () -> CrosshairConfig.cancelCrosshairEventDuringFlight,
                    value -> CrosshairConfig.cancelCrosshairEventDuringFlight = value));
            return new PageView(grid);
        }

        @Override public void apply() { CrosshairConfig.save(); }

        @Override public void cancel() {
            CrosshairConfig.cancelCrosshairEventOnGround = originalGround;
            CrosshairConfig.cancelCrosshairEventDuringFlight = originalFlight;
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
