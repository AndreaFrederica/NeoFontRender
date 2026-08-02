package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Source classification, filtering and player interaction rules. */
final class ChatRulesSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":chat_rules"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.chat.rules.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1041; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final Snapshot original = new Snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid switches = c.grid()
                    .add(c.toggleText(() -> tr("source_classification"), () -> tooltip("source_classification"),
                            () -> EnhancedChatConfig.sourceClassification,
                            value -> EnhancedChatConfig.sourceClassification = value))
                    .add(c.toggleText(() -> tr("block_players"), () -> tooltip("block_players"),
                            () -> EnhancedChatConfig.blockPlayerMessages,
                            value -> EnhancedChatConfig.blockPlayerMessages = value))
                    .add(c.toggleText(() -> tr("block_server"), () -> tooltip("block_server"),
                            () -> EnhancedChatConfig.blockServerMessages,
                            value -> EnhancedChatConfig.blockServerMessages = value))
                    .add(c.toggleText(() -> tr("block_private"), () -> tooltip("block_private"),
                            () -> EnhancedChatConfig.blockPrivateMessages,
                            value -> EnhancedChatConfig.blockPrivateMessages = value))
                    .add(c.toggleText(() -> tr("mention_completion"), () -> tooltip("mention_completion"),
                            () -> EnhancedChatConfig.mentionCompletion,
                            value -> EnhancedChatConfig.mentionCompletion = value))
                    .add(c.toggleText(() -> tr("mention_notification"), () -> tooltip("mention_notification"),
                            () -> EnhancedChatConfig.mentionNotification,
                            value -> EnhancedChatConfig.mentionNotification = value));

            NfrOptionsGrid fields = new NfrOptionsGrid(260, 42, 8, true)
                    .add(field("player_pattern", () -> EnhancedChatConfig.playerSourcePattern,
                            value -> EnhancedChatConfig.playerSourcePattern = value, 1024))
                    .add(field("server_pattern", () -> EnhancedChatConfig.serverSourcePattern,
                            value -> EnhancedChatConfig.serverSourcePattern = value, 1024))
                    .add(field("private_pattern", () -> EnhancedChatConfig.privateSourcePattern,
                            value -> EnhancedChatConfig.privateSourcePattern = value, 1024))
                    .add(field("block_pattern", () -> EnhancedChatConfig.blockedMessagePattern,
                            value -> EnhancedChatConfig.blockedMessagePattern = value, 1024))
                    .add(field("muted_players", () -> EnhancedChatConfig.mutedPlayers,
                            value -> EnhancedChatConfig.mutedPlayers = value, 512))
                    .add(field("mention_sound", () -> EnhancedChatConfig.mentionSound,
                            value -> EnhancedChatConfig.mentionSound = value, 512))
                    .add(field("private_command", () -> EnhancedChatConfig.privateMessageCommand,
                            value -> EnhancedChatConfig.privateMessageCommand = value, 512));
            return new PageView(switches, fields);
        }

        @Override public void apply() { EnhancedChatConfig.save(); }
        @Override public void cancel() { original.restore(); }
    }

    private static NfrLabeledTextField field(String key, Supplier<String> getter,
                                              Consumer<String> setter, int maxLength) {
        TextFieldWidget editor = new TextFieldWidget().setMaxLength(maxLength)
                .value(new NfrStringValue(getter, setter));
        return new NfrLabeledTextField(tr("rules." + key), editor);
    }

    private static final class Snapshot {
        private final boolean sourceClassification = EnhancedChatConfig.sourceClassification;
        private final boolean blockPlayers = EnhancedChatConfig.blockPlayerMessages;
        private final boolean blockServer = EnhancedChatConfig.blockServerMessages;
        private final boolean blockPrivate = EnhancedChatConfig.blockPrivateMessages;
        private final boolean mentionCompletion = EnhancedChatConfig.mentionCompletion;
        private final boolean mentionNotification = EnhancedChatConfig.mentionNotification;
        private final String playerPattern = EnhancedChatConfig.playerSourcePattern;
        private final String serverPattern = EnhancedChatConfig.serverSourcePattern;
        private final String privatePattern = EnhancedChatConfig.privateSourcePattern;
        private final String blockPattern = EnhancedChatConfig.blockedMessagePattern;
        private final String mutedPlayers = EnhancedChatConfig.mutedPlayers;
        private final String mentionSound = EnhancedChatConfig.mentionSound;
        private final String privateCommand = EnhancedChatConfig.privateMessageCommand;

        private void restore() {
            EnhancedChatConfig.sourceClassification = sourceClassification;
            EnhancedChatConfig.blockPlayerMessages = blockPlayers;
            EnhancedChatConfig.blockServerMessages = blockServer;
            EnhancedChatConfig.blockPrivateMessages = blockPrivate;
            EnhancedChatConfig.mentionCompletion = mentionCompletion;
            EnhancedChatConfig.mentionNotification = mentionNotification;
            EnhancedChatConfig.playerSourcePattern = playerPattern;
            EnhancedChatConfig.serverSourcePattern = serverPattern;
            EnhancedChatConfig.privateSourcePattern = privatePattern;
            EnhancedChatConfig.blockedMessagePattern = blockPattern;
            EnhancedChatConfig.mutedPlayers = mutedPlayers;
            EnhancedChatConfig.mentionSound = mentionSound;
            EnhancedChatConfig.privateMessageCommand = privateCommand;
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.chat." + suffix);
    }

    private static String tooltip(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements.tooltip.chat." + suffix);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid switches, NfrOptionsGrid fields) {
            super(section(switches, switches::preferredHeight), section(fields, fields::preferredHeight));
        }
    }
}
