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
    private static int lastLoggedIndex = -1;
    private static boolean renderFailureLogged;
    private static volatile boolean addonConfigLoaded;

    private SplashTipsRenderer() {}

    @SuppressWarnings("unused")
    public static void render(String startupText, Object fontRenderer, int displayWidth, int displayHeight,
                              int fontColor, float alpha) {
        renderInternal(startupText, fontRenderer, displayWidth, displayHeight, fontColor, alpha, true);
    }

    private static void renderInternal(String startupText, Object fontRenderer, int displayWidth,
                                       int displayHeight, int fontColor, float alpha,
                                       boolean modernSplash) {
        ensureAddonConfigLoaded();
        if (!addonConfigFlag("enabled", true)
                || !addonConfigFlag(modernSplash ? "showOnModernSplash" : "showOnForgeLoading", true)) {
            return;
        }
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

            // ModernSplash's fontColor is RGB-only. Carry its frame fade alpha through the
            // color integer as well, because the AWT texture backend resolves its final GL
            // color from this argument.
            int packedColor = (fontColor & 0x00FFFFFF)
                    | (Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24);

            float leftEdge = 320.0F - displayWidth / 2.0F + 4.0F;
            float rightEdge = 320.0F + displayWidth / 2.0F - 4.0F;
            float startupRight = leftEdge + getTextWidth(fontRenderer, startupText) * 2.0F;
            float safeLeft = startupRight + 12.0F;

            int maxWidth = Math.min(280, Math.max(80, (int) ((rightEdge - safeLeft) / 2.0F)));
            String[] lines = wrapText(fontRenderer, text, maxWidth);

            // Match the startup-time baseline. Keep the text centered unless that would overlap
            // the startup timer, in which case reserve the timer's measured width and shift right.
            int lineHeight = 10;
            float baselineY = 240.0F + displayHeight / 2.0F - 20.0F;
            int localY = -(lines.length - 1) * lineHeight;
            int textWidth = maxLineWidth(fontRenderer, lines);
            float centeredX = 320.0F - textWidth;
            float drawX = Math.max(centeredX, safeLeft);

            org.lwjgl.opengl.GL11.glPushMatrix();
            if (!modernSplash) {
                // Forge leaves the Mojang-logo translation/rotation active after drawing the
                // logo. Start tips from the splash projection's screen-space origin instead of
                // inheriting that transform.
                org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
                org.lwjgl.opengl.GL11.glLoadIdentity();
            }
            org.lwjgl.opengl.GL11.glTranslatef(drawX, baselineY, 0.0F);
            try {
                org.lwjgl.opengl.GL11.glScalef(2.0F, 2.0F, 1.0F);
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);

                float red = ((fontColor >> 16) & 0xFF) / 255.0F;
                float green = ((fontColor >> 8) & 0xFF) / 255.0F;
                float blue = (fontColor & 0xFF) / 255.0F;
                org.lwjgl.opengl.GL11.glColor4f(red, green, blue, alpha);

                for (String line : lines) {
                    // Pass the resolved splash color through the same font path used by Forge.
                    // The AWT backend uses this value explicitly, so its output does not depend
                    // on whatever texture/color state the previous splash element left behind.
                    if (modernSplash) {
                    drawText(fontRenderer, line, 0, localY, packedColor);
                    } else {
                        // Forge's SplashFontRenderer is already backed by this same AWT
                        // backend. Call it directly here to avoid another reflective dispatch
                        // while Forge is in its dedicated splash thread.
                        SplashCompat.drawString(line, 0, localY, packedColor);
                    }
                    localY += lineHeight;
                }
            } finally {
                org.lwjgl.opengl.GL11.glPopMatrix();
            }

            renderFailureLogged = false;
            if (lastLoggedIndex != index) {
                LOGGER.info("Rendered tip #{}: '{}' at y={}", index, text,
                        Math.round(baselineY));
                lastLoggedIndex = index;
            }
        } catch (Exception e) {
            if (!renderFailureLogged) {
                LOGGER.warn("Failed to render splash tip; further failures will be suppressed", e);
                renderFailureLogged = true;
            }
        }
    }

    /** Forge's vanilla splash has no ModernSplash alpha/local-variable contract. */
    public static void renderForge(Object fontRenderer, int displayWidth, int displayHeight) {
        // Forge's vanilla splash uses black text on its light background.
        renderInternal(null, fontRenderer, displayWidth, displayHeight, 0xFF000000, 1.0F, false);
    }

    private static synchronized void ensureAddonConfigLoaded() {
        if (addonConfigLoaded) return;
        addonConfigLoaded = true;
        try {
            Class<?> config = Class.forName("neofontrender.addons.tips.TipsConfig");
            config.getMethod("load").invoke(null);
        } catch (Throwable ignored) {
            // UI Enhancements is optional; core splash rendering keeps its defaults.
        }
    }

    private static boolean addonConfigFlag(String field, boolean fallback) {
        try {
            Class<?> config = Class.forName("neofontrender.addons.tips.TipsConfig");
            return config.getField(field).getBoolean(null);
        } catch (Throwable ignored) {
            return fallback;
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

    private static void drawText(Object fontRenderer, String text, int x, int y, int color)
            throws ReflectiveOperationException {
        java.lang.reflect.Method m = fontRenderer.getClass().getDeclaredMethod(
                "func_78276_b", String.class, int.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(fontRenderer, text, x, y, color);
    }

    private static int getTextWidth(Object fontRenderer, String text) {
        try {
            java.lang.reflect.Method m = fontRenderer.getClass().getDeclaredMethod(
                    "func_78256_a", String.class);
            m.setAccessible(true);
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

    private static int maxLineWidth(Object fontRenderer, String[] lines) {
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, getTextWidth(fontRenderer, line));
        }
        return max;
    }
}
