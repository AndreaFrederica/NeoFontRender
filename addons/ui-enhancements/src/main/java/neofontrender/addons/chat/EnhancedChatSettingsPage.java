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

/** Settings page for the vanilla chat module. */
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
        private final boolean originalTabbed = EnhancedChatConfig.tabbedChat;
        private final boolean originalSmooth = SmoothScrollConfigAccess.chatConfigured();
        private final boolean originalAnimateMessages = EnhancedChatConfig.animateMessages;
        private final int originalMessageDuration = EnhancedChatConfig.messageAnimationDuration;
        private final float originalMessageDistance = EnhancedChatConfig.messageAnimationDistance;
        private final String originalMessageEasing = EnhancedChatConfig.messageAnimationEasing;
        private final boolean originalAnimateInput = EnhancedChatConfig.animateInput;
        private final int originalInputDuration = EnhancedChatConfig.inputAnimationDuration;
        private final float originalInputDistance = EnhancedChatConfig.inputAnimationDistance;
        private final String originalInputEasing = EnhancedChatConfig.inputAnimationEasing;

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
                            value -> EnhancedChatConfig.persistSent = value))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.animate_messages"),
                            () -> tr("tooltip.chat.animate_messages"),
                            () -> EnhancedChatConfig.animateMessages,
                            value -> EnhancedChatConfig.animateMessages = value))
                    .add(controls.dropdownText(
                            "chat_message_animation_duration",
                            () -> tr("gui.chat.message_duration"),
                            () -> Integer.toString(EnhancedChatConfig.messageAnimationDuration),
                            value -> EnhancedChatConfig.messageAnimationDuration = Integer.parseInt(value),
                            Arrays.asList("75", "100", "150", "200", "300", "500"),
                            value -> withUnit(value, "milliseconds")).size(260, 24))
                    .add(controls.dropdownText(
                            "chat_message_animation_distance",
                            () -> tr("gui.chat.message_distance"),
                            () -> Float.toString(EnhancedChatConfig.messageAnimationDistance),
                            value -> EnhancedChatConfig.messageAnimationDistance = Float.parseFloat(value),
                            Arrays.asList("3.0", "5.0", "7.0", "9.0", "12.0"),
                            value -> withUnit(value, "pixels")).size(260, 24))
                    .add(controls.dropdownText(
                            "chat_message_animation_easing",
                            () -> tr("gui.chat.message_easing"),
                            () -> EnhancedChatConfig.messageAnimationEasing,
                            value -> EnhancedChatConfig.messageAnimationEasing = value,
                            Arrays.asList("linear", "sine", "quad", "cubic", "back"),
                            EnhancedChatSettingsPage::easingLabel).size(260, 24))
                    .add(controls.toggleText(
                            () -> tr("gui.chat.animate_input"),
                            () -> tr("tooltip.chat.animate_input"),
                            () -> EnhancedChatConfig.animateInput,
                            value -> EnhancedChatConfig.animateInput = value))
                    .add(controls.dropdownText(
                            "chat_input_animation_duration",
                            () -> tr("gui.chat.input_duration"),
                            () -> Integer.toString(EnhancedChatConfig.inputAnimationDuration),
                            value -> EnhancedChatConfig.inputAnimationDuration = Integer.parseInt(value),
                            Arrays.asList("75", "100", "150", "170", "200", "300", "500"),
                            value -> withUnit(value, "milliseconds")).size(260, 24))
                    .add(controls.dropdownText(
                            "chat_input_animation_distance",
                            () -> tr("gui.chat.input_distance"),
                            () -> Float.toString(EnhancedChatConfig.inputAnimationDistance),
                            value -> EnhancedChatConfig.inputAnimationDistance = Float.parseFloat(value),
                            Arrays.asList("3.0", "5.0", "8.0", "10.0", "12.0"),
                            value -> withUnit(value, "pixels")).size(260, 24))
                    .add(controls.dropdownText(
                            "chat_input_animation_easing",
                            () -> tr("gui.chat.input_easing"),
                            () -> EnhancedChatConfig.inputAnimationEasing,
                            value -> EnhancedChatConfig.inputAnimationEasing = value,
                            Arrays.asList("linear", "sine", "quad", "cubic", "back"),
                            EnhancedChatSettingsPage::easingLabel).size(260, 24));
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
            EnhancedChatConfig.animateMessages = originalAnimateMessages;
            EnhancedChatConfig.messageAnimationDuration = originalMessageDuration;
            EnhancedChatConfig.messageAnimationDistance = originalMessageDistance;
            EnhancedChatConfig.messageAnimationEasing = originalMessageEasing;
            EnhancedChatConfig.animateInput = originalAnimateInput;
            EnhancedChatConfig.inputAnimationDuration = originalInputDuration;
            EnhancedChatConfig.inputAnimationDistance = originalInputDistance;
            EnhancedChatConfig.inputAnimationEasing = originalInputEasing;
            SmoothScrollConfigAccess.setChatConfigured(originalSmooth);
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }

    static String easingKey(String value) {
        return "neofontrender_ui_enhancements.gui.chat.easing." + value;
    }

    private static String easingLabel(String value) {
        return AddonI18n.tr(easingKey(value));
    }

    private static String withUnit(String value, String unit) {
        return value + " " + tr("gui.unit." + unit);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) {
            super(section(grid, grid::preferredHeight));
        }
    }
}
