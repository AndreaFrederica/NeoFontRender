package neofontrender.addons.tips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.ModernTextApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the current tip on the loading screen. Uses {@link ModernTextApi} when available
 * to match the loading screen's modern text rendering, falling back to vanilla FontRenderer.
 */
public final class TipRenderer {
    private TipRenderer() {}

    /**
     * Draw the current tip above the loading screen's title area.
     *
     * @param width       screen width
     * @param height      screen height
     * @param margin      left margin (matches loading screen layout)
     * @param titleTop    Y coordinate of the top of the title text
     * @param alpha       0-1 fade alpha
     * @param textColor   ARGB text color
     */
    public static void draw(int width, int height, int margin, int titleTop,
                            float alpha, int textColor) {
        Tip tip = TipManager.INSTANCE.currentTip();
        if (tip == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;

        float tipAlpha = alpha * 0.85F;
        int tipColor = applyAlpha(textColor, tipAlpha);
        int dimColor = applyAlpha(textColor, tipAlpha * 0.6F);

        int maxWidth = Math.min(width - margin * 2, (int) (width * 0.55F));
        String text = tip.text();
        if (text.isEmpty()) return;

        boolean useModern = ModernTextApi.isAvailable();
        float tipFontSize = useModern ? Math.max(1.0F, font.FONT_HEIGHT * 0.85F) : 0;

        String[] lines;
        if (useModern) {
            lines = wrapTextModern(text, maxWidth, tipFontSize);
        } else {
            lines = wrapTextVanilla(text, maxWidth, font);
        }

        float lineSpacing = useModern ? tipFontSize + 2 : font.FONT_HEIGHT + 1;
        float startY = titleTop - lines.length * lineSpacing - 6;

        String title = tip.title();
        if (!title.isEmpty()) {
            if (useModern) {
                ModernTextApi.draw(title, margin, startY, tipFontSize * 1.1F, dimColor);
            } else {
                font.drawString(title, margin, (int) startY, dimColor, false);
            }
            startY += lineSpacing + 2;
        }

        for (String line : lines) {
            if (useModern) {
                ModernTextApi.draw(line, margin, startY, tipFontSize, tipColor);
            } else {
                font.drawString(line, margin, (int) startY, tipColor, false);
            }
            startY += lineSpacing;
        }
    }

    private static String[] wrapTextModern(String text, int maxWidth, float fontSize) {
        String[] words = text.split(" ");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && ModernTextApi.measure(candidate, fontSize) > maxWidth) {
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

    private static String[] wrapTextVanilla(String text, int maxWidth, FontRenderer font) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && font.getStringWidth(candidate) > maxWidth) {
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

    private static int applyAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) ((argb >>> 24) * alpha)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
