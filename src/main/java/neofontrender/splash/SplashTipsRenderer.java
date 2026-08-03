package neofontrender.splash;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Self-contained tip renderer for ModernSplash.
 * Loads tips directly from JSON files and manages its own cycling state,
 * so it works even before any mod's preInit() is called.
 */
public final class SplashTipsRenderer {
    private static final Logger LOGGER = LogManager.getLogger("NFR Tips");
    private static final Gson GSON = new Gson();
    private static final long CYCLE_NANOS = 6_000_000_000L; // 6 seconds

    private static final List<String> tips = new ArrayList<>();
    private static boolean loaded;
    private static int index;
    private static long lastCycle;

    private SplashTipsRenderer() {}

    @SuppressWarnings("unused")
    public static void render(Object fontRenderer, int screenWidth, int screenHeight) {
        if (fontRenderer == null) return;

        try {
            if (!loaded) {
                loadTips();
                loaded = true;
            }
            if (tips.isEmpty()) return;

            long now = System.nanoTime();
            if (now - lastCycle > CYCLE_NANOS) {
                index = (index + 1) % tips.size();
                lastCycle = now;
            }

            String text = tips.get(index);
            if (text == null || text.isEmpty()) return;

            int maxWidth = (int) (screenWidth * 0.55F);
            int margin = 20;
            String[] lines = wrapText(fontRenderer, text, maxWidth);

            int lineHeight = 12;
            int startY = screenHeight - 20 - lines.length * lineHeight - 16;

            // Ensure GL state is correct for text rendering
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
            org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            int color = 0xFFDDDDDD; // fully opaque light gray
            for (String line : lines) {
                drawText(fontRenderer, line, margin, startY, color);
                startY += lineHeight;
            }

            if (index == 0 && lastCycle == 0) {
                LOGGER.info("Rendering tip: {}", text);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to render splash tip", e);
        }
    }

    private static void loadTips() {
        // Try loading from neofontrender_ui_enhancements namespace
        loadFromResource("neofontrender_ui_enhancements", "tips/tips.json");
        // Also try tipsmod namespace for compatibility
        loadFromResource("tipsmod", "tips/tips.json");

        if (!tips.isEmpty()) {
            Collections.shuffle(tips);
            LOGGER.info("Loaded {} tips for ModernSplash display", tips.size());
        }
    }

    private static void loadFromResource(String namespace, String path) {
        try {
            ClassLoader cl = SplashTipsRenderer.class.getClassLoader();
            String resourcePath = "assets/" + namespace + "/" + path;
            InputStream is = cl.getResourceAsStream(resourcePath);
            if (is == null) return;

            JsonObject root = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;

            if (root.has("tips") && root.get("tips").isJsonArray()) {
                for (JsonElement elem : root.getAsJsonArray("tips")) {
                    if (elem.isJsonObject()) {
                        JsonObject tip = elem.getAsJsonObject();
                        String key = extractKey(tip.get("text"));
                        if (key != null && !key.isEmpty()) {
                            // For splash display, just use the raw key as a placeholder.
                            // The JSON lang files may not be loaded yet during splash.
                            // We try to resolve via the JSON lang loader if available.
                            String resolved = tryTranslate(namespace, key);
                            tips.add(resolved != null ? resolved : key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load tips from {}:{}", namespace, path, e);
        }
    }

    private static String extractKey(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("translate")) return obj.get("translate").getAsString();
            if (obj.has("text")) return obj.get("text").getAsString();
        }
        return null;
    }

    private static String tryTranslate(String namespace, String key) {
        try {
            // Try to load the JSON lang file from resources
            String lang = "en_us"; // Default language for splash
            String langPath = "assets/" + namespace + "/lang/" + lang + ".json";
            ClassLoader cl = SplashTipsRenderer.class.getClassLoader();
            InputStream is = cl.getResourceAsStream(langPath);
            if (is != null) {
                JsonObject langObj = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                if (langObj != null && langObj.has(key)) {
                    return langObj.get(key).getAsString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void drawText(Object fontRenderer, String text, int x, int y, int color) {
        try {
            java.lang.reflect.Method m = fontRenderer.getClass().getMethod(
                    "func_78276_b", String.class, int.class, int.class, int.class);
            m.invoke(fontRenderer, text, x, y, color);
        } catch (Exception ignored) {}
    }

    private static int getTextWidth(Object fontRenderer, String text) {
        try {
            java.lang.reflect.Method m = fontRenderer.getClass().getMethod(
                    "func_78256_a", String.class);
            return (int) m.invoke(fontRenderer, text);
        } catch (Exception e) {
            return text.length() * 6;
        }
    }

    private static String[] wrapText(Object fontRenderer, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && getTextWidth(fontRenderer, candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        if (lines.isEmpty()) lines.add("");
        return lines.toArray(new String[0]);
    }
}
