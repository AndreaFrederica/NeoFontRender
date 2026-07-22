package neofontrender.addons.chat;

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

final class ChatStyleSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":chat_style"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.chat_style.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1042; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final Snapshot original = new Snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("enabled"), () -> tr("enabled.tooltip"),
                            () -> ChatStyleConfig.enabled, value -> ChatStyleConfig.enabled = value))
                    .add(c.dropdownText("chat_style_border_width", () -> tr("border_width"),
                            () -> Integer.toString(ChatStyleConfig.borderWidth),
                            value -> ChatStyleConfig.borderWidth = Integer.parseInt(value),
                            Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8"), value -> value).size(260, 24))
                    .add(c.dropdownText("chat_style_opacity", () -> tr("opacity"),
                            () -> Integer.toString(ChatStyleConfig.opacityPercent),
                            value -> ChatStyleConfig.opacityPercent = Integer.parseInt(value),
                            Arrays.asList("10", "20", "30", "40", "50", "60", "70", "80", "90", "100"),
                            value -> value + "%").size(260, 24))
                    .add(color(c, "chat_style_background", "background", () -> ChatStyleConfig.background, value -> ChatStyleConfig.background = value))
                    .add(color(c, "chat_style_border", "border", () -> ChatStyleConfig.border, value -> ChatStyleConfig.border = value))
                    .add(color(c, "chat_style_input", "input_background", () -> ChatStyleConfig.inputBackground, value -> ChatStyleConfig.inputBackground = value))
                    .add(color(c, "chat_style_tray", "tray_background", () -> ChatStyleConfig.trayBackground, value -> ChatStyleConfig.trayBackground = value))
                    .add(color(c, "chat_style_tab", "tab_background", () -> ChatStyleConfig.tabBackground, value -> ChatStyleConfig.tabBackground = value))
                    .add(color(c, "chat_style_active", "active_tab", () -> ChatStyleConfig.activeTab, value -> ChatStyleConfig.activeTab = value))
                    .add(color(c, "chat_style_unread", "unread_tab", () -> ChatStyleConfig.unreadTab, value -> ChatStyleConfig.unreadTab = value))
                    .add(color(c, "chat_style_pinged", "pinged_tab", () -> ChatStyleConfig.pingedTab, value -> ChatStyleConfig.pingedTab = value))
                    .add(color(c, "chat_style_hovered", "hovered_tab", () -> ChatStyleConfig.hoveredTab, value -> ChatStyleConfig.hoveredTab = value))
                    .add(color(c, "chat_style_scrollbar", "scrollbar", () -> ChatStyleConfig.scrollbar, value -> ChatStyleConfig.scrollbar = value))
                    .add(color(c, "chat_style_text", "text", () -> ChatStyleConfig.text, value -> ChatStyleConfig.text = value));
            return new PageView(grid);
        }

        @Override public void apply() { ChatStyleConfig.save(); }
        @Override public void cancel() { original.restore(); }

        private static IWidget color(NfrSettingsControls c, String id, String key,
                                     java.util.function.IntSupplier getter,
                                     java.util.function.IntConsumer setter) {
            return c.colorText(id, () -> tr(key), getter, setter, true).size(260, 24);
        }
    }

    private static final class Snapshot {
        private final boolean enabled = ChatStyleConfig.enabled;
        private final int[] colors = {ChatStyleConfig.background, ChatStyleConfig.border, ChatStyleConfig.inputBackground,
                ChatStyleConfig.trayBackground, ChatStyleConfig.tabBackground, ChatStyleConfig.activeTab,
                ChatStyleConfig.unreadTab, ChatStyleConfig.pingedTab, ChatStyleConfig.hoveredTab,
                ChatStyleConfig.scrollbar, ChatStyleConfig.text};
        private final int borderWidth = ChatStyleConfig.borderWidth;
        private final int opacity = ChatStyleConfig.opacityPercent;

        private void restore() {
            ChatStyleConfig.enabled = enabled;
            ChatStyleConfig.background = colors[0]; ChatStyleConfig.border = colors[1];
            ChatStyleConfig.inputBackground = colors[2]; ChatStyleConfig.trayBackground = colors[3];
            ChatStyleConfig.tabBackground = colors[4]; ChatStyleConfig.activeTab = colors[5];
            ChatStyleConfig.unreadTab = colors[6]; ChatStyleConfig.pingedTab = colors[7];
            ChatStyleConfig.hoveredTab = colors[8]; ChatStyleConfig.scrollbar = colors[9];
            ChatStyleConfig.text = colors[10]; ChatStyleConfig.borderWidth = borderWidth;
            ChatStyleConfig.opacityPercent = opacity;
        }
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements.gui.chat_style." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
