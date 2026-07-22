package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import net.minecraft.client.resources.I18n;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;

/** Settings page for the vanilla chat module. */
final class EnhancedChatSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":enhanced_chat"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.chat.category"; }
    @Override public int order() { return 1040; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalEnabled = EnhancedChatConfig.enabled;
        private final boolean originalExtended = EnhancedChatConfig.extendedHistory;
        private final int originalLimit = EnhancedChatConfig.maxMessages;
        private final boolean originalPersistence = EnhancedChatConfig.persistence;
        private final boolean originalReceived = EnhancedChatConfig.persistReceived;
        private final boolean originalSent = EnhancedChatConfig.persistSent;
        private final boolean originalTabbed = EnhancedChatConfig.tabbedChat;
        private final boolean originalSmooth = SmoothScrollConfigAccess.chatConfigured();

        @Override
        public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid()
                    .add(controls.toggleText(
                            () -> tr("gui.chat.enabled"),
                            () -> tr("tooltip.chat.enabled"),
                            () -> EnhancedChatConfig.enabled,
                            value -> EnhancedChatConfig.enabled = value))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.extended_history"),
                            () -> tr("tooltip.chat.extended_history"),
                            () -> EnhancedChatConfig.extendedHistory,
                            value -> EnhancedChatConfig.extendedHistory = value))
                    .add(controls.dropdownText(
                            "chat_history_limit",
                            () -> tr("gui.chat.history_limit"),
                            () -> Integer.toString(EnhancedChatConfig.maxMessages),
                            value -> EnhancedChatConfig.maxMessages = Integer.parseInt(value),
                            Arrays.asList("100", "500", "1000", "4096", "8192", "16384", "32767"),
                            value -> value).size(260, 24))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.smooth_scrolling"),
                            () -> tr("tooltip.chat.smooth_scrolling"),
                            SmoothScrollConfigAccess::chatConfigured,
                            SmoothScrollConfigAccess::setChatConfigured))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.tabbed"),
                            () -> tr("tooltip.chat.tabbed"),
                            () -> EnhancedChatConfig.tabbedChat,
                            value -> EnhancedChatConfig.tabbedChat = value))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.persistence"),
                            () -> tr("tooltip.chat.persistence"),
                            () -> EnhancedChatConfig.persistence,
                            value -> EnhancedChatConfig.persistence = value))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.persist_received"),
                            () -> tr("tooltip.chat.persist_received"),
                            () -> EnhancedChatConfig.persistReceived,
                            value -> EnhancedChatConfig.persistReceived = value))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.persist_sent"),
                            () -> tr("tooltip.chat.persist_sent"),
                            () -> EnhancedChatConfig.persistSent,
                            value -> EnhancedChatConfig.persistSent = value));
            return new PageView(grid);
        }

        @Override
        public void apply() {
            SmoothScrollConfigAccess.save();
            EnhancedChatConfig.save();
        }

        @Override
        public void cancel() {
            restoreOriginal();
        }

        public void rollbackApply() {
            restoreOriginal();
            SmoothScrollConfigAccess.save();
            EnhancedChatConfig.save();
        }

        private void restoreOriginal() {
            EnhancedChatConfig.enabled = originalEnabled;
            EnhancedChatConfig.extendedHistory = originalExtended;
            EnhancedChatConfig.maxMessages = originalLimit;
            EnhancedChatConfig.persistence = originalPersistence;
            EnhancedChatConfig.persistReceived = originalReceived;
            EnhancedChatConfig.persistSent = originalSent;
            EnhancedChatConfig.tabbedChat = originalTabbed;
            SmoothScrollConfigAccess.setChatConfigured(originalSmooth);
        }
    }

    private static String tr(String suffix) {
        return I18n.format("neofontrender_ui_enhancements." + suffix);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) {
            super(section(grid, grid::preferredHeight));
        }
    }
}
