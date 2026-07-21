package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.settings.AdvancedSettings;
import mnm.mods.tabbychat.settings.GeneralSettings;
import mnm.mods.tabbychat.settings.TabbySettings;
import mnm.mods.tabbychat.util.ChatVisibility;
import mnm.mods.tabbychat.util.TimeStamps;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;
import java.util.stream.Collectors;

final class TabbedChatSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":tabbed_chat"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.tabby.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1041; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final TabbySettings settings = TabbyChat.getInstance().settings;
        private final GeneralSettings general = settings.general;
        private final AdvancedSettings advanced = settings.advanced;
        private final boolean logChat = general.logChat.get();
        private final boolean splitLog = general.splitLog.get();
        private final boolean timestamps = general.timestampChat.get();
        private final TimeStamps timestampStyle = general.timestampStyle.get();
        private final boolean antiSpam = general.antiSpam.get();
        private final double antiSpamPrejudice = general.antiSpamPrejudice.get();
        private final boolean unreadFlashing = general.unreadFlashing.get();
        private final boolean spelling = advanced.spelling.get();
        private final boolean keepOpen = advanced.keepChatOpen.get();
        private final boolean hideTag = advanced.hideTag.get();
        private final float unfocusedHeight = advanced.unfocHeight.get();
        private final int fadeTime = advanced.fadeTime.get();
        private final ChatVisibility visibility = advanced.visibility.get();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid()
                    .add(toggle(c, "log_chat", general.logChat::get, general.logChat::set))
                    .add(toggle(c, "split_log", general.splitLog::get, general.splitLog::set))
                    .add(toggle(c, "timestamps", general.timestampChat::get, general.timestampChat::set))
                    .add(c.dropdownText("tabby_timestamp_style", () -> tr("timestamp_style"),
                            () -> general.timestampStyle.get().name(),
                            value -> general.timestampStyle.set(TimeStamps.valueOf(value)),
                            Arrays.stream(TimeStamps.values()).map(Enum::name).collect(Collectors.toList()),
                            value -> TimeStamps.valueOf(value).toString()).size(260, 24))
                    .add(toggle(c, "anti_spam", general.antiSpam::get, general.antiSpam::set))
                    .add(c.dropdownText("tabby_antispam", () -> tr("anti_spam_tolerance"),
                            () -> Double.toString(general.antiSpamPrejudice.get()),
                            value -> general.antiSpamPrejudice.set(Double.parseDouble(value)),
                            Arrays.asList("0.0", "0.1", "0.25", "0.5", "0.75", "1.0"), value -> value).size(260, 24))
                    .add(toggle(c, "unread_flashing", general.unreadFlashing::get, general.unreadFlashing::set))
                    .add(toggle(c, "spelling", advanced.spelling::get, advanced.spelling::set))
                    .add(toggle(c, "keep_open", advanced.keepChatOpen::get, advanced.keepChatOpen::set))
                    .add(toggle(c, "hide_tag", advanced.hideTag::get, advanced.hideTag::set))
                    .add(c.dropdownText("tabby_unfocused_height", () -> tr("unfocused_height"),
                            () -> Float.toString(advanced.unfocHeight.get()),
                            value -> advanced.unfocHeight.set(Float.parseFloat(value)),
                            Arrays.asList("0.25", "0.5", "0.75", "1.0"), value -> value).size(260, 24))
                    .add(c.dropdownText("tabby_fade", () -> tr("fade_time"),
                            () -> Integer.toString(advanced.fadeTime.get()),
                            value -> advanced.fadeTime.set(Integer.parseInt(value)),
                            Arrays.asList("0", "100", "200", "400", "800", "1200"), value -> value).size(260, 24))
                    .add(c.dropdownText("tabby_visibility", () -> tr("visibility"),
                            () -> advanced.visibility.get().name(),
                            value -> advanced.visibility.set(ChatVisibility.valueOf(value)),
                            Arrays.stream(ChatVisibility.values()).map(Enum::name).collect(Collectors.toList()), value -> value).size(260, 24));
            return new PageView(grid);
        }

        private IWidget toggle(NfrSettingsControls c, String key,
                               java.util.function.Supplier<Boolean> getter,
                               java.util.function.Consumer<Boolean> setter) {
            return c.toggleText(() -> tr(key), () -> "", getter, setter);
        }

        @Override public void apply() { settings.saveConfig(); }

        @Override public void cancel() {
            general.logChat.set(logChat);
            general.splitLog.set(splitLog);
            general.timestampChat.set(timestamps);
            general.timestampStyle.set(timestampStyle);
            general.antiSpam.set(antiSpam);
            general.antiSpamPrejudice.set(antiSpamPrejudice);
            general.unreadFlashing.set(unreadFlashing);
            advanced.spelling.set(spelling);
            advanced.keepChatOpen.set(keepOpen);
            advanced.hideTag.set(hideTag);
            advanced.unfocHeight.set(unfocusedHeight);
            advanced.fadeTime.set(fadeTime);
            advanced.visibility.set(visibility);
        }
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements.gui.tabby." + key); }
    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
