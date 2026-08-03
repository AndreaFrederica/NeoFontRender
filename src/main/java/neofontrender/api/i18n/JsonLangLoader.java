package neofontrender.api.i18n;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads JSON lang files as a fallback for MC 1.12.2's {@code I18n.format()}.
 * <p>
 * MC 1.12.2 only loads {@code .lang} (properties-style) files natively.
 * This class parses {@code assets/<namespace>/lang/<locale>.json} files
 * and serves translations when {@code I18n.format()} returns the raw key.
 * <p>
 * Register namespaces via {@link #registerNamespace(String)} and call
 * {@link #translate(String)} instead of {@code I18n.format()}.
 * The reload listener is registered automatically on first use.
 */
public enum JsonLangLoader implements IResourceManagerReloadListener {
    INSTANCE;

    private static final Gson GSON = new Gson();

    private final Set<String> namespaces = ConcurrentHashMap.newKeySet();
    private final Map<String, String> translations = new HashMap<>();
    private String loadedLanguage = "";
    private boolean listenerRegistered;

    /**
     * Register a namespace whose JSON lang files will be loaded.
     * Call this during mod init.
     */
    public void registerNamespace(String namespace) {
        namespaces.add(namespace);
        ensureListenerRegistered();
    }

    public void unregisterNamespace(String namespace) {
        namespaces.remove(namespace);
    }

    /**
     * Translate a key. Tries vanilla {@code I18n} first, then falls back
     * to loaded JSON lang entries.
     */
    public String translate(String key) {
        String result = I18n.format(key);
        if (!result.equals(key)) return result;

        ensureLanguageLoaded();
        return translations.getOrDefault(key, key);
    }

    /**
     * Translate a key using only the JSON lang fallback (skips vanilla I18n).
     */
    public String translateJsonOnly(String key) {
        ensureLanguageLoaded();
        return translations.getOrDefault(key, key);
    }

    private void ensureListenerRegistered() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.getResourceManager() instanceof net.minecraft.client.resources.IReloadableResourceManager) {
            ((net.minecraft.client.resources.IReloadableResourceManager) mc.getResourceManager())
                    .registerReloadListener(this);
        }
    }

    private void ensureLanguageLoaded() {
        Minecraft mc = Minecraft.getMinecraft();
        String lang = mc.gameSettings != null ? mc.gameSettings.language : "en_us";
        // Resource reloads can briefly expose no language files. Do not permanently cache
        // that failed attempt; the next loading-screen frame will retry.
        if (!lang.equals(loadedLanguage) || translations.isEmpty()) {
            loadedLanguage = lang;
            loadLanguage(lang);
        }
    }

    private void loadLanguage(String language) {
        translations.clear();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getResourceManager() == null) return;

        for (String namespace : namespaces) {
            ResourceLocation langFile = new ResourceLocation(
                    namespace, "lang/" + language + ".json");
            if (!loadJsonLang(mc.getResourceManager(), langFile) && !"en_us".equals(language)) {
                ResourceLocation fallback = new ResourceLocation(namespace, "lang/en_us.json");
                loadJsonLang(mc.getResourceManager(), fallback);
            }
        }
    }

    private boolean loadJsonLang(IResourceManager rm, ResourceLocation file) {
        try {
            IResource resource = rm.getResource(file);
            try (InputStream is = resource.getInputStream()) {
                JsonObject root = GSON.fromJson(
                        new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                if (root != null) {
                    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            translations.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }
            return true;
        } catch (Exception ignored) {
            // File doesn't exist for this language, skip
            return false;
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        loadedLanguage = "";
        translations.clear();
    }
}
