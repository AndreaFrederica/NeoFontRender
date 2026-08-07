package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;

/** General chat behavior. Rules and visual options live on their own focused pages. */
final class EnhancedChatSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":enhanced_chat"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.chat.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1040; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalEnabled = EnhancedChatConfig.enabled;
        private final boolean originalExtended = EnhancedChatConfig.extendedHistory;
        private final int originalLimit = EnhancedChatConfig.maxMessages;
        private final boolean originalPersistence = EnhancedChatConfig.persistence;
        private final boolean originalReceived = EnhancedChatConfig.persistReceived;
        private final boolean originalSent = EnhancedChatConfig.persistSent;
        private final boolean originalCopySelection = EnhancedChatConfig.copySelection;
        private final boolean originalCopyFormattingCodes = EnhancedChatConfig.copyFormattingCodes;
        private final boolean originalAmpersandFormatting = EnhancedChatConfig.ampersandFormatting;
        private final boolean originalTabbed = EnhancedChatConfig.tabbedChat;
        private final boolean originalSmooth = SmoothScrollConfigAccess.chatConfigured();
        private final boolean originalSearch = EnhancedChatConfig.messageSearch;
        private final boolean originalCommandCompletion = EnhancedChatConfig.commandCompletion;
        private final boolean originalPrivateCommandBlock = EnhancedChatConfig.privateCommandBlock;
        private final boolean originalForceServerTranslations =
                EnhancedChatConfig.salutationForceServerTranslations;
        private final boolean originalDisableSalutationOverride =
                EnhancedChatConfig.salutationDisableOverride;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid core = c.grid()
                    .add(c.toggleText(() -> tr("gui.chat.enabled"), () -> tr("tooltip.chat.enabled"),
                            () -> EnhancedChatConfig.enabled, value -> EnhancedChatConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.chat.tabbed"), () -> tr("tooltip.chat.tabbed"),
                            () -> EnhancedChatConfig.tabbedChat, value -> EnhancedChatConfig.tabbedChat = value))
                    .add(c.toggleText(() -> tr("gui.chat.smooth_scrolling"),
                            () -> tr("tooltip.chat.smooth_scrolling"),
                            SmoothScrollConfigAccess::chatConfigured,
                            SmoothScrollConfigAccess::setChatConfigured))
                    .add(c.toggleText(() -> tr("gui.chat.search"), () -> tr("tooltip.chat.search"),
                            () -> EnhancedChatConfig.messageSearch,
                            value -> EnhancedChatConfig.messageSearch = value))
                    .add(c.toggleText(() -> tr("gui.chat.command_completion"),
                            () -> tr("tooltip.chat.command_completion"),
                            () -> EnhancedChatConfig.commandCompletion,
                            value -> EnhancedChatConfig.commandCompletion = value))
                    .add(c.toggleText(() -> tr("gui.chat.private_command_block"),
                            () -> tr("tooltip.chat.private_command_block"),
                            () -> EnhancedChatConfig.privateCommandBlock,
                            value -> EnhancedChatConfig.privateCommandBlock = value))
                    .add(c.toggleText(() -> tr("gui.chat.salutation_force_server_translations"),
                            () -> tr("tooltip.chat.salutation_force_server_translations"),
                            () -> EnhancedChatConfig.salutationForceServerTranslations,
                            value -> EnhancedChatConfig.salutationForceServerTranslations = value))
                    .add(c.toggleText(() -> tr("gui.chat.salutation_disable_override"),
                            () -> tr("tooltip.chat.salutation_disable_override"),
                            () -> EnhancedChatConfig.salutationDisableOverride,
                            value -> EnhancedChatConfig.salutationDisableOverride = value));
            NfrOptionsGrid history = c.grid()
                    .add(c.toggleText(() -> tr("gui.chat.extended_history"),
                            () -> tr("tooltip.chat.extended_history"),
                            () -> EnhancedChatConfig.extendedHistory,
                            value -> EnhancedChatConfig.extendedHistory = value))
                    .add(c.dropdownText("chat_history_limit", () -> tr("gui.chat.history_limit"),
                            () -> Integer.toString(EnhancedChatConfig.maxMessages),
                            value -> EnhancedChatConfig.maxMessages = Integer.parseInt(value),
                            Arrays.asList("100", "500", "1000", "4096", "8192", "16384", "32767"),
                            value -> value).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.chat.persistence"),
                            () -> tr("tooltip.chat.persistence"),
                            () -> EnhancedChatConfig.persistence,
                            value -> EnhancedChatConfig.persistence = value))
                    .add(c.toggleText(() -> tr("gui.chat.persist_received"),
                            () -> tr("tooltip.chat.persist_received"),
                            () -> EnhancedChatConfig.persistReceived,
                            value -> EnhancedChatConfig.persistReceived = value))
                    .add(c.toggleText(() -> tr("gui.chat.persist_sent"),
                            () -> tr("tooltip.chat.persist_sent"),
                            () -> EnhancedChatConfig.persistSent,
                            value -> EnhancedChatConfig.persistSent = value));
            NfrOptionsGrid copying = c.grid()
                    .add(c.toggleText(() -> tr("gui.chat.copy_selection"),
                            () -> tr("tooltip.chat.copy_selection"),
                            () -> EnhancedChatConfig.copySelection,
                            value -> EnhancedChatConfig.copySelection = value))
                    .add(c.toggleText(() -> tr("gui.chat.copy_formatting"),
                            () -> tr("tooltip.chat.copy_formatting"),
                            () -> EnhancedChatConfig.copyFormattingCodes,
                            value -> EnhancedChatConfig.copyFormattingCodes = value))
                    .add(c.toggleText(() -> tr("gui.chat.copy_ampersand"),
                            () -> tr("tooltip.chat.copy_ampersand"),
                            () -> EnhancedChatConfig.ampersandFormatting,
                            value -> EnhancedChatConfig.ampersandFormatting = value));
            return new PageView(core, history, copying);
        }

        @Override public void apply() {
            SmoothScrollConfigAccess.save();
            EnhancedChatConfig.save();
        }

        @Override public void cancel() {
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
            EnhancedChatConfig.copySelection = originalCopySelection;
            EnhancedChatConfig.copyFormattingCodes = originalCopyFormattingCodes;
            EnhancedChatConfig.ampersandFormatting = originalAmpersandFormatting;
            EnhancedChatConfig.tabbedChat = originalTabbed;
            EnhancedChatConfig.messageSearch = originalSearch;
            EnhancedChatConfig.commandCompletion = originalCommandCompletion;
            EnhancedChatConfig.privateCommandBlock = originalPrivateCommandBlock;
            EnhancedChatConfig.salutationForceServerTranslations = originalForceServerTranslations;
            EnhancedChatConfig.salutationDisableOverride = originalDisableSalutationOverride;
            SmoothScrollConfigAccess.setChatConfigured(originalSmooth);
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid core, NfrOptionsGrid history, NfrOptionsGrid copying) {
            super(section(core, core::preferredHeight), section(history, history::preferredHeight),
                    section(copying, copying::preferredHeight));
        }
    }
}
