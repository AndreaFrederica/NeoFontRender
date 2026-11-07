package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Self-contained fallback when an old resource loader misses an addon's locale during startup. */
public final class AddonI18n {
    public static final String DOMAIN = "neofontrender_ui_enhancements";
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

    /** Supplies bundled translations to Locale through its normal resource-loading path. */
    public static List<IResource> bundledLanguageResources(List<String> languages) {
        List<IResource> resources = new ArrayList<>();
        boolean hasEnglish = false;
        for (String language : languages) hasEnglish |= "en_us".equals(language);
        if (!hasEnglish) addResource(resources, "en_us");
        for (String language : languages) addResource(resources, language);
        return resources;
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

    private static void addResource(List<IResource> resources, String language) {
        if (AddonI18n.class.getResource(ROOT + language + ".lang") != null) {
            resources.add(new BundledLanguageResource(language));
        }
    }

    private static final class BundledLanguageResource implements IResource {
        private final String language;
        private final ResourceLocation location;

        private BundledLanguageResource(String language) {
            this.language = language;
            this.location = new ResourceLocation(DOMAIN, "lang/" + language + ".lang");
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return location;
        }

        @Override
        public InputStream getInputStream() {
            InputStream stream = AddonI18n.class.getResourceAsStream(ROOT + language + ".lang");
            if (stream == null) throw new IllegalStateException("Missing bundled language " + language);
            return stream;
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return "NeoFontRender UI Enhancements";
        }

        @Override
        public void close() {
        }
    }
}
