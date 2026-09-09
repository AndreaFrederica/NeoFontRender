package neofontrender.addons.typst;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Locale;

/** Fallback for Cleanroom resource reloads that omit an addon's lang domain. */
final class TypstI18n {
    private static final String ROOT = "/assets/neofontrender_typst_renderer/lang/";
    private static String loadedLanguage;
    private static Properties fallback = new Properties();

    private TypstI18n() {}

    static String tr(String key, Object... args) {
        String translated = I18n.hasKey(key) ? I18n.format(key, args) : null;
        if (translated != null && !translated.equals(key)) return translated;
        String language = Minecraft.getMinecraft().gameSettings.language;
        if (!language.equals(loadedLanguage)) load(language);
        String value = fallback.getProperty(key, key);
        return args.length == 0 ? value : String.format(Locale.ROOT, value, args);
    }

    private static synchronized void load(String language) {
        Properties values = new Properties();
        read(values, "en_us");
        if (!"en_us".equals(language)) read(values, language);
        fallback = values;
        loadedLanguage = language;
    }

    private static void read(Properties values, String language) {
        try (InputStream stream = TypstI18n.class.getResourceAsStream(ROOT + language + ".lang")) {
            if (stream != null) values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // Keep the key visible if the optional locale resource is unavailable.
        }
    }
}
