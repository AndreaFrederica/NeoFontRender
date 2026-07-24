package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Self-contained fallback when an old resource loader misses an addon's locale during startup. */
public final class AddonI18n {
    private static final String ROOT = "/assets/neofontrender_ui_enhancements/lang/";
    private static String loadedLanguage;
    private static Properties fallback = new Properties();

    private AddonI18n() {}

    public static String tr(String key) {
        String translated = I18n.format(key);
        if (!key.equals(translated)) return translated;
        String language = Minecraft.getMinecraft().gameSettings.language;
        if (!language.equals(loadedLanguage)) load(language);
        return fallback.getProperty(key, key);
    }

    private static synchronized void load(String language) {
        if (language.equals(loadedLanguage)) return;
        fallback = loadLanguage(language);
        loadedLanguage = language;
    }

    static Properties loadLanguage(String language) {
        Properties values = new Properties();
        read(values, "en_US");
        String selectedLanguage = canonicalLanguage(language);
        if (!"en_US".equals(selectedLanguage)) read(values, selectedLanguage);
        return values;
    }

    private static String canonicalLanguage(String language) {
        if ("en_us".equalsIgnoreCase(language)) return "en_US";
        if ("zh_cn".equalsIgnoreCase(language)) return "zh_CN";
        return language;
    }

    private static void read(Properties values, String language) {
        try (InputStream stream = AddonI18n.class.getResourceAsStream(ROOT + language + ".lang")) {
            if (stream != null) values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            TooltipModule.LOGGER.warn("Failed to load addon language {}", language, exception);
        }
    }
}
