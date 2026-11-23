package neofontrender.splash;

import org.lwjgl.opengl.GL11;

import java.util.Locale;

/** Prevents known native driver crashes before the splash backend uploads its first texture. */
final class SplashDriverSafety {
    static final String DISABLE_INTEL_AWT_PROPERTY =
            "neofontrender.splash.disableIntelAwtUpload";

    private SplashDriverSafety() {}

    static boolean shouldDisableAwtUpload() {
        if (!Boolean.getBoolean(DISABLE_INTEL_AWT_PROPERTY)) return false;
        if (!classPresent("org.lwjgl.opengl.GL11C")) return false;
        String vendor;
        String renderer;
        try {
            vendor = GL11.glGetString(GL11.GL_VENDOR);
            renderer = GL11.glGetString(GL11.GL_RENDERER);
        } catch (Throwable ignored) {
            return false;
        }
        return unsafeIntelCleanroom(System.getProperty("os.name", ""), vendor, renderer);
    }

    static boolean unsafeIntelCleanroom(String osName, String vendor, String renderer) {
        String os = normalize(osName);
        String gpu = normalize(vendor) + " " + normalize(renderer);
        return os.contains("windows") && gpu.contains("intel");
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, SplashDriverSafety.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
