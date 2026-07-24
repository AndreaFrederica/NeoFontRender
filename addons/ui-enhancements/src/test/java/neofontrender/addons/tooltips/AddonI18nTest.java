package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AddonI18nTest {
    @Test
    void bundledLanguagesCoverEverySettingsTranslation() {
        Set<String> expected = expectedKeys();
        Properties english = AddonI18n.loadLanguage("en_US");
        Properties chinese = AddonI18n.loadLanguage("zh_CN");

        assertEquals(expected, english.stringPropertyNames());
        assertEquals(expected, chinese.stringPropertyNames());
        for (String key : expected) {
            assertFalse(english.getProperty(key).trim().isEmpty(), key);
            assertFalse(chinese.getProperty(key).trim().isEmpty(), key);
        }
        assertNotEquals(english.getProperty("neofontrender_ui_enhancements.gui.enabled"),
                chinese.getProperty("neofontrender_ui_enhancements.gui.enabled"));
    }

    @Test
    void unavailableLocaleFallsBackToEnglishResource() {
        Properties english = AddonI18n.loadLanguage("en_US");
        Properties unavailable = AddonI18n.loadLanguage("fr_FR");

        assertEquals(english, unavailable);
    }

    @Test
    void legacyLowercaseLocaleNamesResolveBundledResources() {
        assertEquals(AddonI18n.loadLanguage("en_US"), AddonI18n.loadLanguage("en_us"));
        assertEquals(AddonI18n.loadLanguage("zh_CN"), AddonI18n.loadLanguage("zh_cn"));
    }

    private static Set<String> expectedKeys() {
        Set<String> keys = new HashSet<>();
        add(keys, "neofontrender_ui_enhancements.",
                "name tooltip scrolling input effects info.version info.description");
        add(keys, "neofontrender_ui_enhancements.gui.",
                "tooltips.category enabled style style.modernui style.mica style.legacy legendary obscure_yield "
                        + "nei_custom mod_name mod_name_format mod_name_format.none mod_name_format.blue_italic "
                        + "mod_name_format.gray_italic mod_name_format.dark_gray_italic mod_name_format.aqua_italic "
                        + "mod_name_format.gold_italic mod_name_format.blue mod_name_format.gray rounded center_title "
                        + "title_break adaptive_border border_shading border_shading.gradient border_shading.solid "
                        + "border_shading.horizontal border_shading.vertical border_shading.spectrum border_cycle "
                        + "corner_radius border_width shadow_radius shadow_alpha shadow_x shadow_y shadow_steps "
                        + "corner_segments aa_width text_shadow divider_alpha horizontal_padding vertical_padding "
                        + "line_height title_gap cursor_offset max_width unlimited fill_color border_color corner.ul "
                        + "corner.ur corner.lr corner.ll shadow_color text_color title_color");
        add(keys, "neofontrender_ui_enhancements.tooltip.",
                "enabled legendary obscure_yield nei_custom mod_name");
        add(keys, "neofontrender_ui_enhancements.gui.scrolling.",
                "category enabled vanilla_lists forge_lists creative_inventory chat duration step");
        add(keys, "neofontrender_ui_enhancements.tooltip.scrolling.",
                "enabled vanilla_lists forge_lists creative_inventory chat");
        add(keys, "neofontrender_ui_enhancements.gui.chat.",
                "category enabled tabbed extended_history history_limit smooth_scrolling persistence persist_received "
                        + "persist_sent animate_messages message_duration message_distance message_easing animate_input "
                        + "input_duration input_distance input_easing easing.linear easing.sine easing.quad easing.cubic easing.back");
        add(keys, "neofontrender_ui_enhancements.tooltip.chat.",
                "enabled tabbed extended_history smooth_scrolling persistence persist_received persist_sent "
                        + "animate_messages animate_input");
        add(keys, "neofontrender_ui_enhancements.gui.tabby.",
                "category log_chat split_log timestamps timestamp_style anti_spam anti_spam_tolerance unread_flashing "
                        + "spelling keep_open hide_tag unfocused_height fade_time visibility visibility.always "
                        + "visibility.normal visibility.hidden");
        add(keys, "neofontrender_ui_enhancements.gui.chat_style.",
                "category enabled enabled.tooltip border_width opacity background border input_background tray_background "
                        + "tab_background active_tab unread_tab pinged_tab hovered_tab scrollbar text");
        add(keys, "neofontrender_ui_enhancements.gui.input.", "category ibeam");
        add(keys, "neofontrender_ui_enhancements.tooltip.input.", "ibeam");
        add(keys, "neofontrender_ui_enhancements.gui.effects.",
                "category enabled fade fade_duration blur blur_radius gradient color");
        add(keys, "neofontrender_ui_enhancements.tooltip.effects.", "enabled blur");
        add(keys, "neofontrender_ui_enhancements.gui.hud.",
                "category enabled yield_classic health absorption armor food air mount numbers smooth rounded width height gap "
                        + "background border health_low health_high absorption_color armor_color food_color saturation_color "
                        + "theme theme.modern theme.flat theme.glass theme.segmented theme.minimal theme.classic air_color mount_color");
        add(keys, "neofontrender_ui_enhancements.tooltip.hud.",
                "enabled yield_classic health absorption armor food air mount numbers smooth rounded");
        add(keys, "neofontrender_ui_enhancements.gui.unit.", "pixels milliseconds");
        return keys;
    }

    private static void add(Set<String> keys, String prefix, String suffixes) {
        Arrays.stream(suffixes.split(" ")).map(prefix::concat).forEach(keys::add);
    }
}
