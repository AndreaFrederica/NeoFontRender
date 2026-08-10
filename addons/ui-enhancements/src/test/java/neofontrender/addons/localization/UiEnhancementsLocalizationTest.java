package neofontrender.addons.localization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEnhancementsLocalizationTest {
    private static final String ROOT = "/assets/neofontrender_ui_enhancements/";

    @Test
    void englishAndChineseUiLangFilesHaveTheSameKeys() throws Exception {
        Properties english = lang("en_us");
        Properties chinese = lang("zh_cn");
        assertEquals(english.stringPropertyNames(), chinese.stringPropertyNames());
        for (String key : new String[]{
                "neofontrender_ui_enhancements.gui.crosshair.width",
                "neofontrender_ui_enhancements.gui.crosshair.drawn_size",
                "neofontrender_ui_enhancements.gui.flight_roll.hud_reload",
                "neofontrender_ui_enhancements.info.version",
                "neofontrender_ui_enhancements.info.description"}) {
            assertTrue(english.containsKey(key), key);
            assertTrue(chinese.containsKey(key), key);
        }
    }

    @Test
    void everyCameraToggleHasLocalizedTooltip() throws Exception {
        Properties english = lang("en_us");
        Properties chinese = lang("zh_cn");
        for (String name : new String[]{
                "f5_enabled", "f5_shoulder", "f5_freelook", "f5_drone",
                "f5_replace_third", "f5_skip_front", "f5_remember", "freelook_toggle",
                "freelook_collision", "drone_collision", "drone_interaction",
                "shoulder_unlimited_x", "shoulder_unlimited_y", "shoulder_unlimited_z",
                "shoulder_valkyrien_collision", "shoulder_collision", "shoulder_climbing",
                "shoulder_dynamic_offsets", "shoulder_limit_reach", "shoulder_custom_ray",
                "shoulder_transparency", "shoulder_hide_player"}) {
            String key = "neofontrender_ui_enhancements.tooltip.camera." + name;
            assertTrue(english.containsKey(key), "Missing English camera tooltip: " + key);
            assertTrue(chinese.containsKey(key), "Missing Chinese camera tooltip: " + key);
        }
    }

    @Test
    void everyTipTranslationExistsInBothJsonLocales() throws Exception {
        JsonObject english = json(ROOT + "lang/en_us.json");
        JsonObject chinese = json(ROOT + "lang/zh_cn.json");
        assertEquals(english.keySet(), chinese.keySet());

        JsonObject catalog = json(ROOT + "tips/tips.json");
        for (JsonElement element : catalog.getAsJsonArray("tips")) {
            String key = element.getAsJsonObject().getAsJsonObject("text").get("translate").getAsString();
            assertTrue(english.has(key), "Missing English tip: " + key);
            assertTrue(chinese.has(key), "Missing Chinese tip: " + key);
        }
    }

    private static Properties lang(String locale) throws Exception {
        Properties result = new Properties();
        try (InputStream stream = resource(ROOT + "lang/" + locale + ".lang")) {
            result.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static JsonObject json(String path) throws Exception {
        try (InputStream stream = resource(path);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = UiEnhancementsLocalizationTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        return stream;
    }
}
