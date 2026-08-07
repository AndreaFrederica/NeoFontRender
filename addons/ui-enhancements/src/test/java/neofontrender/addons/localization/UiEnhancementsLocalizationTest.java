package neofontrender.addons.localization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEnhancementsLocalizationTest {
    private static final String ROOT = "/assets/neofontrender_ui_enhancements/";

    @Test
    void englishAndChineseUiLangFilesHaveTheSameKeys() throws Exception {
        Properties english = lang("en_US");
        Properties chinese = lang("zh_CN");
        assertEquals(english.stringPropertyNames(), chinese.stringPropertyNames());
        for (String key : new String[]{
                "neofontrender_ui_enhancements.name",
                "neofontrender_ui_enhancements.gui.chat.enabled",
                "neofontrender_ui_enhancements.gui.effects.enabled",
                "neofontrender_ui_enhancements.info.version",
                "neofontrender_ui_enhancements.info.description"}) {
            assertTrue(english.containsKey(key), key);
            assertTrue(chinese.containsKey(key), key);
        }
    }

    @Test
    void everyTipTranslationExistsInBothJsonLocales() throws Exception {
        JsonObject english = json(ROOT + "lang/en_us.json");
        JsonObject chinese = json(ROOT + "lang/zh_cn.json");
        assertEquals(keys(english), keys(chinese));

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

    private static Set<String> keys(JsonObject object) {
        return object.entrySet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static InputStream resource(String path) {
        InputStream stream = UiEnhancementsLocalizationTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        return stream;
    }
}
