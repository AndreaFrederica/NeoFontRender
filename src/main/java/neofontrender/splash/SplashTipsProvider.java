package neofontrender.splash;

/**
 * Provider interface for tips displayed on the ModernSplash loading screen.
 * Implemented by UIE's tips module and registered via {@link #setInstance(Provider)}.
 * <p>
 * This interface lives in NFR core so that the ASM-injected tip renderer
 * can access tips without depending on UIE classes directly.
 */
public final class SplashTipsProvider {
    private static volatile Provider provider;

    public interface Provider {
        /** Returns the current tip text, or null if no tip is available. */
        String currentTipText();
        /** Called once per frame to advance the tip cycle. */
        void tick();
    }

    public static void setInstance(Provider p) {
        provider = p;
    }

    public static Provider get() {
        return provider;
    }
}
