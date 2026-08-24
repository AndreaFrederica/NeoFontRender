package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Self-contained fallback when an old resource loader misses an addon's locale during startup. */
public final class AddonI18n {
    private static final String ROOT = "/assets/neofontrender_ui_enhancements/lang/";
    private static String loadedLanguage;
    private static Properties fallback = new Properties();

    private AddonI18n() {}

    public static String tr(String key) {
        if (I18n.hasKey(key)) return I18n.format(key);
        String language = Minecraft.getMinecraft().gameSettings.language;
        if (!language.equals(loadedLanguage)) load(language);
        return fallback.getProperty(key, key);
    }

    /**
     * Merges the addon's bundled translations into the vanilla locale table, so
     * vanilla-rendered strings (e.g. keybinding names in the controls screen)
     * resolve even when the resource loader skips this addon's lang domain.
     * Languages are applied in order, with en_us guaranteed as the base.
     */
    public static void mergeInto(Map<String, String> target, List<String> languages) {
        boolean hasEnglish = false;
        for (String language : languages) hasEnglish |= "en_us".equals(language);
        if (!hasEnglish) merge(target, "en_us");
        for (String language : languages) merge(target, language);
    }

    private static void merge(Map<String, String> target, String language) {
        Properties values = new Properties();
        read(values, language);
        for (String key : values.stringPropertyNames()) {
            target.put(key, values.getProperty(key));
        }
    }

    private static synchronized void load(String language) {
        if (language.equals(loadedLanguage)) return;
        Properties values = new Properties();
        read(values, "en_us");
        if (!"en_us".equals(language)) read(values, language);
        fallback = values;
        loadedLanguage = language;
    }

    private static void read(Properties values, String language) {
        try (InputStream stream = AddonI18n.class.getResourceAsStream(ROOT + language + ".lang")) {
            if (stream != null) values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            TooltipModule.LOGGER.warn("Failed to load addon language {}", language, exception);
        }
    }
}
