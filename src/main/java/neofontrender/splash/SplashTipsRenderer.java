package neofontrender.splash;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Renders tips on ModernSplash's loading screen.
 * Called from the patched {@code SplashProgress$2.run()} rendering loop.
 * <p>
 * ModernSplash uses a 640×480 orthographic coordinate system with origin at top-left.
 */
public final class SplashTipsRenderer {
    private static final Logger LOGGER = LogManager.getLogger("NFR Tips");

    private SplashTipsRenderer() {}

    /**
     * Called from the patched SplashProgress$2.run() rendering loop.
     * Must be called while ModernSplash's GL context is current.
     *
     * @param fontRenderer the SplashFontRenderer instance (NFR-patched)
     * @param screenWidth  screen width (640 in ModernSplash's coord system)
     * @param screenHeight screen height (480)
     */
    @SuppressWarnings("unused")
    public static void render(Object fontRenderer, int screenWidth, int screenHeight) {
        SplashTipsProvider.Provider provider = SplashTipsProvider.get();
        if (provider == null || fontRenderer == null) return;

        try {
            provider.tick();
            String text = provider.currentTipText();
            if (text == null || text.isEmpty()) return;

            int maxWidth = (int) (screenWidth * 0.55F);
            int margin = 20;
            int bottomMargin = 20;

            String[] lines = wrapText(fontRenderer, text, maxWidth);

            int lineHeight = 12;
            int totalHeight = lines.length * lineHeight;
            int startY = screenHeight - bottomMargin - totalHeight - 16;

            int color = 0xCCDDDDDD;
            for (String line : lines) {
                drawText(fontRenderer, line, margin, startY, color);
                startY += lineHeight;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to render splash tip", e);
        }
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
        java.util.List<String> lines = new java.util.ArrayList<>();
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
