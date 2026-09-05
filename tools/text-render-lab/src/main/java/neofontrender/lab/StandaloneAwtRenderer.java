package neofontrender.lab;

import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;
import neofontrender.text.InlineSpan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Headless-capable AWT raster adapter used by the standalone laboratory and golden tests. */
public final class StandaloneAwtRenderer {
    private static final String OBFUSCATED =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public Result render(StructuredText text, Settings settings) {
        Settings value = settings == null ? Settings.defaults() : settings;
        BufferedImage image = new BufferedImage(value.width, value.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        List<String> trace = new ArrayList<>();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(value.background);
            graphics.fillRect(0, 0, value.width, value.height);
            Font base = new Font(value.fontFamily, Font.PLAIN, value.fontSize);
            int x = value.padding;
            int y = value.padding + graphics.getFontMetrics(base).getAscent();
            int maxX = Math.max(x + 1, value.width - value.padding);
            int lineHeight = graphics.getFontMetrics(base).getHeight() + value.lineGap;
            trace.add("input -> syntax:" + String.join(",", text.appliedSyntaxProviderIds()));
            trace.add("structured -> styles=" + text.styles().size()
                    + ",effects=" + text.effects().size());
            if (text.animated()) trace.add("structured -> animated-glyph-resolver:frame=" + value.frameId);
            if (!text.appliedMiddlewareIds().isEmpty()) {
                trace.add("structured-middleware -> " + String.join(",", text.appliedMiddlewareIds()));
            }

            int inlineDraws = 0;

            for (int index = 0; index < text.plainText().length();) {
                InlineSpan inline = InlineRenderSupport.at(text, index);
                if (inline != null) {
                    int advance = InlineRenderSupport.width(inline.content(), value.fontSize);
                    if (x + advance > maxX && x > value.padding) {
                        x = value.padding;
                        y += lineHeight;
                    }
                    int rgb = text.styleAt(index).hasColorOverride()
                            ? 0xFF000000 | text.styleAt(index).rgb() : value.foreground.getRGB();
                    if (value.shadow) InlineRenderSupport.draw(graphics, inline.content(), x, y,
                            value.fontSize, rgb, true);
                    InlineRenderSupport.draw(graphics, inline.content(), x, y,
                            value.fontSize, rgb, false);
                    x += advance;
                    index = inline.end();
                    inlineDraws++;
                    continue;
                }
                int codePoint = text.plainText().codePointAt(index);
                int count = Character.charCount(codePoint);
                if (codePoint == '\n') {
                    x = value.padding;
                    y += lineHeight;
                    index += count;
                    continue;
                }
                TextStyle style = text.styleAt(index);
                int awtStyle = (style.bold() ? Font.BOLD : Font.PLAIN)
                        | (style.italic() ? Font.ITALIC : Font.PLAIN);
                Font font = base.deriveFont(awtStyle);
                graphics.setFont(font);
                String glyph = new String(Character.toChars(codePoint));
                if (style.obfuscated() && !Character.isWhitespace(codePoint)) {
                    glyph = obfuscatedGlyph(graphics.getFontMetrics(font), glyph,
                            value.frameId, index);
                }
                FontMetrics metrics = graphics.getFontMetrics(font);
                int advance = metrics.stringWidth(glyph);
                if (x + advance > maxX && x > value.padding) {
                    x = value.padding;
                    y += lineHeight;
                }
                if (y > value.height - value.padding) break;
                paintEffectBackgrounds(graphics, text.effects(), index, x, y,
                        advance, metrics.getHeight());
                int rgb = style.hasColorOverride() ? style.rgb() : value.foreground.getRGB();
                if (value.shadow) {
                    graphics.setColor(new Color(0, 0, 0, 96));
                    graphics.drawString(glyph, x + 1, y + 1);
                }
                graphics.setColor(new Color(rgb | 0xFF000000, true));
                graphics.drawString(glyph, x, y);
                graphics.setStroke(new BasicStroke(Math.max(1.0F, value.fontSize / 20.0F)));
                if (style.underline()) graphics.drawLine(x, y + 2, x + advance, y + 2);
                if (style.strikethrough()) {
                    int strikeY = y - metrics.getAscent() / 3;
                    graphics.drawLine(x, strikeY, x + advance, strikeY);
                }
                x += advance;
                index += count;
            }
            trace.add("inline-content -> draws=" + inlineDraws);
            trace.add("raster:awt-standalone -> framebuffer:buffered-image");
        } finally {
            graphics.dispose();
        }
        return new Result(image, trace);
    }

    private static String obfuscatedGlyph(FontMetrics metrics, String original,
                                          long frame, int sourceIndex) {
        int target = metrics.stringWidth(original);
        List<Character> candidates = new ArrayList<>();
        int bestDelta = Integer.MAX_VALUE;
        for (char candidate : OBFUSCATED.toCharArray()) {
            int delta = Math.abs(metrics.charWidth(candidate) - target);
            if (delta < bestDelta) {
                candidates.clear();
                bestDelta = delta;
            }
            if (delta == bestDelta) candidates.add(candidate);
        }
        Random random = new Random(frame * 0x9E3779B97F4A7C15L + sourceIndex * 31L);
        return Character.toString(candidates.get(random.nextInt(candidates.size())));
    }

    private static void paintEffectBackgrounds(Graphics2D graphics,
                                               List<StructuredEffectSpan> effects,
                                               int index, int x, int baseline,
                                               int width, int height) {
        for (StructuredEffectSpan effect : effects) {
            if (index < effect.start() || index >= effect.end()) continue;
            String glow = effect.parameters().get("glowColor");
            if (glow == null || glow.length() != 8) continue;
            try {
                int value = (int) Long.parseLong(glow, 16);
                if ((value >>> 24) == 0) continue;
                graphics.setColor(new Color(value, true));
                graphics.fillRect(x - 2, baseline - height + 3, width + 4, height + 3);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public static final class Settings {
        public final int width, height, fontSize, padding, lineGap;
        public final long frameId;
        public final String fontFamily;
        public final Color background, foreground;
        public final boolean shadow;

        public Settings(int width, int height, int fontSize, int padding, int lineGap,
                        long frameId, String fontFamily, Color background,
                        Color foreground, boolean shadow) {
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            this.fontSize = Math.max(1, fontSize);
            this.padding = Math.max(0, padding);
            this.lineGap = Math.max(0, lineGap);
            this.frameId = frameId;
            this.fontFamily = fontFamily == null || fontFamily.isEmpty()
                    ? Font.SANS_SERIF : fontFamily;
            this.background = background == null ? new Color(28, 30, 34) : background;
            this.foreground = foreground == null ? new Color(235, 238, 242) : foreground;
            this.shadow = shadow;
        }

        public static Settings defaults() {
            return new Settings(720, 430, 28, 26, 12, 0L,
                    Font.SANS_SERIF, null, null, true);
        }
    }

    public static final class Result {
        public final BufferedImage image;
        public final List<String> trace;

        private Result(BufferedImage image, List<String> trace) {
            this.image = image;
            this.trace = Collections.unmodifiableList(new ArrayList<>(trace));
        }
    }
}
