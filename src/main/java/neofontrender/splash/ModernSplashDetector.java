package neofontrender.splash;

/**
 * Detects whether ModernSplash is present on the classpath.
 *
 * <p>We check for the source class {@code gkappa.modernsplash.CustomSplash} rather than the
 * remapped {@code net.minecraftforge.fml.client.SplashProgress}, because our transformer must
 * act on the source class before ModernSplash renames it.</p>
 */
public final class ModernSplashDetector {

    private static final String CUSTOM_SPLASH_CLASS = "gkappa.modernsplash.CustomSplash";
    private static Boolean installed;

    private ModernSplashDetector() {}

    public static boolean isInstalled() {
        if (installed != null) {
            return installed;
        }
        try {
            Class.forName(CUSTOM_SPLASH_CLASS, false, ModernSplashDetector.class.getClassLoader());
            installed = Boolean.TRUE;
        } catch (ClassNotFoundException ignored) {
            installed = Boolean.FALSE;
        }
        return installed;
    }
}
