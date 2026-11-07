package neofontrender.addons.notifications;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scrollable settings page generated from the pack-owned notification JSON. */
public final class CompatibilityNotificationsSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":compatibility_notifications"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.notifications.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1998; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final Map<String, Boolean> original = snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid();
            for (CompatibilityNotification notice : CompatibilityNotificationRegistry.all()) {
                grid.add(controls.toggleText(
                        () -> I18nValue.of(notice.resolvedTitleKey(), notice.modid),
                        () -> I18nValue.of(notice.resolvedMessageKey(), notice.modid),
                        () -> notice.enabled,
                        value -> notice.enabled = value));
            }
            return new PageView(grid);
        }

        @Override public void apply() { CompatibilityNotificationRegistry.save(); }

        @Override public void cancel() {
            for (CompatibilityNotification notice : CompatibilityNotificationRegistry.all()) {
                Boolean value = original.get(notice.resolvedId());
                if (value != null) notice.enabled = value;
            }
        }

        private static Map<String, Boolean> snapshot() {
            Map<String, Boolean> result = new LinkedHashMap<>();
            for (CompatibilityNotification notice : CompatibilityNotificationRegistry.all()) {
                result.put(notice.resolvedId(), notice.enabled);
            }
            return result;
        }
    }

    private static final class I18nValue {
        private static String of(String key, String fallback) {
            String value = AddonI18n.tr(key);
            return key.equals(value) ? fallback : value;
        }
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) {
            super(section(grid, grid::preferredHeight));
        }
    }
}
