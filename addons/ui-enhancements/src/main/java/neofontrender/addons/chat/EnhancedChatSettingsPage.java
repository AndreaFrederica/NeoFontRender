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

final class EnhancedChatSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":enhanced_chat"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.chat.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1040; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean enabled = EnhancedChatConfig.enabled;
        private final boolean tabbed = EnhancedChatConfig.tabbedChat;
        private final boolean extended = EnhancedChatConfig.extendedHistory;
        private final int limit = EnhancedChatConfig.maxMessages;
        private final boolean persistence = EnhancedChatConfig.persistence;
        private final boolean received = EnhancedChatConfig.persistReceived;
        private final boolean sent = EnhancedChatConfig.persistSent;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.chat.enabled"), () -> tr("tooltip.chat.enabled"),
                            () -> EnhancedChatConfig.enabled, value -> EnhancedChatConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.chat.tabbed"), () -> tr("tooltip.chat.tabbed"),
                            () -> EnhancedChatConfig.tabbedChat, value -> EnhancedChatConfig.tabbedChat = value))
                    .add(c.toggleText(() -> tr("gui.chat.extended_history"), () -> tr("tooltip.chat.extended_history"),
                            () -> EnhancedChatConfig.extendedHistory, value -> EnhancedChatConfig.extendedHistory = value))
                    .add(c.dropdownText("chat_history_limit", () -> tr("gui.chat.history_limit"),
                            () -> Integer.toString(EnhancedChatConfig.maxMessages),
                            value -> EnhancedChatConfig.maxMessages = Integer.parseInt(value),
                            Arrays.asList("100", "500", "1000", "4096", "8192", "16384", "32767"), value -> value)
                            .size(260, 24))
                    .add(c.toggleText(() -> tr("gui.chat.persistence"), () -> tr("tooltip.chat.persistence"),
                            () -> EnhancedChatConfig.persistence, value -> EnhancedChatConfig.persistence = value))
                    .add(c.toggleText(() -> tr("gui.chat.persist_received"), () -> "",
                            () -> EnhancedChatConfig.persistReceived, value -> EnhancedChatConfig.persistReceived = value))
                    .add(c.toggleText(() -> tr("gui.chat.persist_sent"), () -> "",
                            () -> EnhancedChatConfig.persistSent, value -> EnhancedChatConfig.persistSent = value));
            return new PageView(grid);
        }

        @Override public void apply() { EnhancedChatConfig.save(); }

        @Override public void cancel() {
            EnhancedChatConfig.enabled = enabled;
            EnhancedChatConfig.tabbedChat = tabbed;
            EnhancedChatConfig.extendedHistory = extended;
            EnhancedChatConfig.maxMessages = limit;
            EnhancedChatConfig.persistence = persistence;
            EnhancedChatConfig.persistReceived = received;
            EnhancedChatConfig.persistSent = sent;
        }
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
