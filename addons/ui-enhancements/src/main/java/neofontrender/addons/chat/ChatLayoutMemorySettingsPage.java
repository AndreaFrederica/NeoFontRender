package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrSettingsCard;
import neofontrender.client.gui.views.NfrContentView;

import java.util.HashSet;
import java.util.Set;

final class ChatLayoutMemorySettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":chat_layout_memory"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.chat_memory.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1043; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private boolean enabled = ChatLayoutMemoryController.enabled();
        private final Set<ChatLayoutMemory.Viewport> deleted = new HashSet<>();
        private boolean clear;
        private boolean dirty;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            var c = context.controls();
            var grid = c.grid().add(c.toggleText(() -> tr("enabled"), () -> tr("help"),
                    () -> enabled, value -> { enabled = value; dirty = true; }, () -> {}));
            grid.add(c.action(() -> tr("clear"), 260, 24, () -> { clear = true; dirty = true; context.refresh(); }));
            var current = ChatLayoutMemoryController.viewport();
            grid.add(new NfrSettingsCard(() -> tr("current"),
                    () -> current == null ? tr("unavailable") : label(current)));
            var profiles = ChatLayoutMemoryController.snapshot();
            profiles.keySet().removeAll(deleted);
            if (clear) profiles.clear();
            if (profiles.isEmpty()) grid.add(new NfrSettingsCard(() -> tr("empty"), () -> tr("empty_help")));
            profiles.forEach((v, b) -> grid.add(new NfrSettingsCard(
                    () -> label(v) + (v.equals(current) ? "  " + tr("active") : ""),
                    () -> tr("position") + " " + b.x() + ", " + b.y() + "    " + tr("size")
                            + " " + b.width() + " × " + b.height())
                    .action(c.action(() -> tr("delete"), 180, 24,
                            () -> { deleted.add(v); dirty = true; context.refresh(); }))));
            return new PageView(grid);
        }

        @Override public void apply() {
            if (!dirty) return;
            // Merge only deletions into the live map: resizing while settings are open may add profiles.
            var profiles = ChatLayoutMemoryController.snapshot();
            if (clear) profiles.clear();
            else profiles.keySet().removeAll(deleted);
            ChatLayoutMemoryController.configure(enabled, profiles);
            deleted.clear();
            clear = false;
            dirty = false;
        }

        @Override public void cancel() { /* The draft never mutates live settings. */ }
    }

    private static String label(ChatLayoutMemory.Viewport v) {
        return v.width() + " × " + v.height() + "  GUI ×" + v.guiScale()
                + "  " + tr("chat_scale") + " " + (v.chatScale() / 10.0F) + "%";
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements.gui.chat_memory." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
