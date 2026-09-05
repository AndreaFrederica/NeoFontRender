package neofontrender.lab;

import neofontrender.core.font.cosmic.CosmicNative;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;
import neofontrender.text.InlineSpan;
import neofontrender.text.layout.CjkLineBreakProvider;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Minecraft-free adapter around the production cosmic-text JNI rasterizer. */
public final class StandaloneCosmicRenderer implements AutoCloseable {
    private static final int RASTER_MAGIC = 0x434F534D;
    private static final String OBFUSCATED =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final Map<String, Float> measureCache = new HashMap<>();
    private long engine;
    private String status = "not initialized";

    public synchronized boolean isAvailable() {
        return ensureEngine();
    }

    public synchronized String status() {
        ensureEngine();
        return status;
    }

    public synchronized Result render(StructuredText text,
                                      StandaloneAwtRenderer.Settings settings) {
        StandaloneAwtRenderer.Settings value = settings == null
                ? StandaloneAwtRenderer.Settings.defaults() : settings;
        BufferedImage canvas = new BufferedImage(value.width, value.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        List<String> trace = new ArrayList<>();
        try {
            graphics.setColor(value.background);
            graphics.fillRect(0, 0, value.width, value.height);
            if (!ensureEngine()) {
                trace.add("raster:cosmic-native -> unavailable:" + status);
                paintUnavailable(graphics, value, status);
                return new Result(canvas, trace, false, status);
            }

            trace.add("input -> syntax:" + String.join(",", text.appliedSyntaxProviderIds()));
            trace.add("structured -> styles=" + text.styles().size()
                    + ",effects=" + text.effects().size());
            if (text.animated()) {
                trace.add("structured -> animated-glyph-resolver:frame=" + value.frameId);
            }
            if (!text.appliedMiddlewareIds().isEmpty()) {
                trace.add("structured-middleware -> " + String.join(",", text.appliedMiddlewareIds()));
            }

            Set<Integer> breakOpportunities = new HashSet<>(
                    new CjkLineBreakProvider().opportunities(text));
            float x = value.padding;
            float baseline = value.padding + value.fontSize * 1.15F;
            float lineHeight = value.fontSize * 1.4F + value.lineGap;
            float maxX = Math.max(x + 1.0F, value.width - value.padding);
            int nativeRuns = 0;
            int effectRuns = 0;
            int inlineDraws = 0;

            String plain = text.plainText();
            for (int index = 0; index < plain.length();) {
                InlineSpan inline = InlineRenderSupport.at(text, index);
                if (inline != null) {
                    int advance = InlineRenderSupport.width(inline.content(), value.fontSize);
                    if (x + advance > maxX && x > value.padding) {
                        x = value.padding;
                        baseline += lineHeight;
                    }
                    TextStyle inlineStyle = text.styleAt(index);
                    int argb = inlineStyle.hasColorOverride() ? 0xFF000000 | inlineStyle.rgb()
                            : value.foreground.getRGB();
                    if (value.shadow) InlineRenderSupport.draw(graphics, inline.content(),
                            Math.round(x), Math.round(baseline), value.fontSize, argb, true);
                    InlineRenderSupport.draw(graphics, inline.content(), Math.round(x),
                            Math.round(baseline), value.fontSize, argb, false);
                    x += advance;
                    index = inline.end();
                    inlineDraws++;
                    continue;
                }
                int codePoint = plain.codePointAt(index);
                if (codePoint == '\n') {
                    x = value.padding;
                    baseline += lineHeight;
                    index += Character.charCount(codePoint);
                    continue;
                }
                if (baseline - value.fontSize > value.height - value.padding) break;

                TextStyle style = text.styleAt(index);
                StructuredEffectSpan effect = effectAt(text.effects(), index);
                int boundary = nextBoundary(text, index, style, effect);
                while (index < boundary) {
                    float available = maxX - x;
                    int end = fittingEnd(plain, index, boundary, style, value,
                            available, breakOpportunities);
                    if (end == index && x > value.padding) {
                        x = value.padding;
                        baseline += lineHeight;
                        continue;
                    }
                    if (end == index) end = nextCodePoint(plain, index);

                    String visible = transformed(plain, index, end, style, value.frameId);
                    int argb = style.hasColorOverride() ? 0xFF000000 | style.rgb()
                            : value.foreground.getRGB();
                    if (effect != null) {
                        argb = parseHex(effect.parameters().get("textColor"), argb);
                    }
                    Raster raster = raster(visible, argb, styleFlags(style), value.fontSize);
                    int drawX = Math.round(x + raster.offsetX / raster.scale);
                    int drawY = Math.round(baseline + raster.offsetY / raster.scale);
                    if (value.shadow) drawTinted(graphics, raster.image, drawX + 1, drawY + 1,
                            0x60000000);
                    if (effect != null) {
                        paintEffect(graphics, raster.image, drawX, drawY, effect);
                        effectRuns++;
                    }
                    graphics.drawImage(raster.image, drawX, drawY, null);
                    x += raster.advance;
                    nativeRuns++;
                    boolean wrapped = end < boundary;
                    index = end;
                    if (wrapped) {
                        x = value.padding;
                        baseline += lineHeight;
                    }
                }
            }
            trace.add("layout:cjk-aware -> width=" + value.width);
            trace.add("raster:cosmic-native -> runs=" + nativeRuns + ",family="
                    + CosmicNative.primaryFamily(engine));
            trace.add("postprocess:brilliant-preview -> runs=" + effectRuns);
            trace.add("inline-content -> draws=" + inlineDraws);
            trace.add("framebuffer:buffered-image");
            return new Result(canvas, trace, true, status);
        } catch (RuntimeException | LinkageError error) {
            status = error.toString();
            trace.add("raster:cosmic-native -> error:" + status);
            paintUnavailable(graphics, value, status);
            return new Result(canvas, trace, false, status);
        } finally {
            graphics.dispose();
        }
    }

    private int fittingEnd(String text, int start, int boundary, TextStyle style,
                           StandaloneAwtRenderer.Settings settings, float available,
                           Set<Integer> opportunities) {
        String complete = transformed(text, start, boundary, style, settings.frameId);
        int flags = styleFlags(style);
        if (measure(complete, flags, settings.fontSize) <= available) return boundary;

        int fitted = start;
        int cursor = nextCodePoint(text, start);
        while (cursor <= boundary) {
            String candidate = transformed(text, start, cursor, style, settings.frameId);
            if (measure(candidate, flags, settings.fontSize) > available) break;
            fitted = cursor;
            if (cursor == boundary) break;
            cursor = nextCodePoint(text, cursor);
        }
        if (fitted == start) return start;

        int preferred = fitted;
        for (int point = start + 1; point <= fitted; point++) {
            if (opportunities.contains(point)
                    || Character.isWhitespace(text.charAt(point - 1))) preferred = point;
        }
        return preferred;
    }

    private float measure(String text, int flags, int fontSize) {
        String key = flags + ":" + fontSize + ':' + text;
        Float cached = measureCache.get(key);
        if (cached != null) return cached;
        float value = CosmicNative.measureSized(engine, text, flags, fontSize);
        measureCache.put(key, value);
        return value;
    }

    private Raster raster(String text, int argb, int flags, int fontSize) {
        byte[] encoded = CosmicNative.renderSized(engine, text, argb, flags, fontSize, 1.0F);
        if (encoded == null || encoded.length < 36) {
            throw new IllegalStateException("Cosmic returned a truncated raster");
        }
        ByteBuffer data = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);
        if (data.getInt() != RASTER_MAGIC) {
            throw new IllegalStateException("Cosmic returned an invalid raster header");
        }
        int width = data.getInt();
        int height = data.getInt();
        int offsetX = data.getInt();
        int offsetY = data.getInt();
        float advance = data.getFloat();
        data.getFloat(); // baseline is represented by offsetY for standalone placement
        float scale = data.getFloat();
        data.getInt(); // raster model flags
        long pixels = (long) width * height;
        if (width < 0 || height < 0 || pixels > Integer.MAX_VALUE
                || data.remaining() != pixels * 4L) {
            throw new IllegalStateException("Cosmic returned invalid dimensions "
                    + width + 'x' + height);
        }
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height),
                BufferedImage.TYPE_INT_ARGB);
        if (pixels > 0) {
            int[] argbPixels = new int[(int) pixels];
            for (int index = 0; index < argbPixels.length; index++) argbPixels[index] = data.getInt();
            image.setRGB(0, 0, width, height, argbPixels, 0, width);
        }
        return new Raster(image, advance, offsetX, offsetY, scale);
    }

    private static int nextBoundary(StructuredText text, int index, TextStyle style,
                                    StructuredEffectSpan effect) {
        int result = text.plainText().length();
        int newline = text.plainText().indexOf('\n', index);
        if (newline >= 0) result = newline;
        for (int cursor = index + 1; cursor < result; cursor++) {
            if (InlineRenderSupport.at(text, cursor) != null) return cursor;
            if (!style.equals(text.styleAt(cursor)) || effectAt(text.effects(), cursor) != effect) {
                return cursor;
            }
        }
        return result;
    }

    private static StructuredEffectSpan effectAt(List<StructuredEffectSpan> effects, int index) {
        for (StructuredEffectSpan effect : effects) {
            if (index >= effect.start() && index < effect.end()) return effect;
        }
        return null;
    }

    private String transformed(String text, int start, int end, TextStyle style, long frame) {
        if (!style.obfuscated()) return text.substring(start, end);
        StringBuilder result = new StringBuilder(end - start);
        for (int index = start; index < end;) {
            int codePoint = text.codePointAt(index);
            int count = Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) result.appendCodePoint(codePoint);
            else result.append(obfuscatedGlyph(new String(Character.toChars(codePoint)),
                    styleFlags(style), frame, index));
            index += count;
        }
        return result.toString();
    }

    private char obfuscatedGlyph(String original, int flags, long frame, int index) {
        float target = measure(original, flags, 32);
        List<Character> candidates = new ArrayList<>();
        float best = Float.MAX_VALUE;
        for (char candidate : OBFUSCATED.toCharArray()) {
            float delta = Math.abs(measure(Character.toString(candidate), flags, 32) - target);
            if (delta + 0.001F < best) {
                candidates.clear();
                best = delta;
            }
            if (Math.abs(delta - best) <= 0.001F) candidates.add(candidate);
        }
        Random random = new Random(frame * 0x9E3779B97F4A7C15L + index * 31L);
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static int nextCodePoint(String text, int index) {
        return Math.min(text.length(), index + Character.charCount(text.codePointAt(index)));
    }

    private static int styleFlags(TextStyle style) {
        return (style.bold() ? 1 : 0) | (style.italic() ? 2 : 0)
                | (style.underline() ? 4 : 0) | (style.strikethrough() ? 8 : 0);
    }

    private static void paintEffect(Graphics2D graphics, BufferedImage source, int x, int y,
                                    StructuredEffectSpan effect) {
        int glow = parseHex(effect.parameters().get("glowColor"), 0);
        int outline = parseHex(effect.parameters().get("outlineColor"), 0);
        if ((glow >>> 24) != 0) {
            for (int offset = 4; offset >= 2; offset--) {
                int alpha = Math.max(1, (glow >>> 24) / (offset * 2));
                int color = alpha << 24 | glow & 0xFFFFFF;
                drawTinted(graphics, source, x - offset, y, color);
                drawTinted(graphics, source, x + offset, y, color);
                drawTinted(graphics, source, x, y - offset, color);
                drawTinted(graphics, source, x, y + offset, color);
            }
        }
        if ((outline >>> 24) != 0) {
            drawTinted(graphics, source, x - 1, y, outline);
            drawTinted(graphics, source, x + 1, y, outline);
            drawTinted(graphics, source, x, y - 1, outline);
            drawTinted(graphics, source, x, y + 1, outline);
        }
    }

    private static void drawTinted(Graphics2D graphics, BufferedImage source, int x, int y,
                                   int argb) {
        BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        int[] pixels = source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0,
                source.getWidth());
        int tintAlpha = argb >>> 24;
        for (int index = 0; index < pixels.length; index++) {
            int alpha = (pixels[index] >>> 24) * tintAlpha / 255;
            pixels[index] = alpha << 24 | argb & 0xFFFFFF;
        }
        tinted.setRGB(0, 0, source.getWidth(), source.getHeight(), pixels, 0, source.getWidth());
        graphics.drawImage(tinted, x, y, null);
    }

    private static int parseHex(String value, int fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void paintUnavailable(Graphics2D graphics,
                                         StandaloneAwtRenderer.Settings settings,
                                         String message) {
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(new Color(255, 110, 100));
        graphics.drawString("Cosmic unavailable", settings.padding, settings.padding + 18);
        graphics.setColor(new Color(220, 220, 220));
        graphics.drawString(message, settings.padding, settings.padding + 40);
    }

    private boolean ensureEngine() {
        if (engine != 0L) return true;
        CosmicRuntime.Result runtime = CosmicRuntime.ensureLoaded();
        if (!runtime.available) {
            status = runtime.message;
            return false;
        }
        try {
            byte[] textFont = CosmicRuntime.readResource(
                    "/assets/neofontrender/fonts/noto_sans_sc-regular.otf");
            byte[] emojiFont = CosmicRuntime.readResource(
                    "/assets/neofontrender/fonts/noto_color_emoji_regular.ttf");
            engine = CosmicNative.createEngine(new byte[][]{textFont, emojiFont},
                    new String[]{"noto_sans_sc-regular.otf", "noto_color_emoji_regular.ttf"},
                    "", new String[0], "", "", "", "", false, 0,
                    28.0F, Locale.getDefault().toLanguageTag());
            if (engine == 0L) throw new IllegalStateException("Cosmic returned a null engine");
            status = runtime.message + ", family=" + CosmicNative.primaryFamily(engine);
            String warnings = CosmicNative.resolutionWarnings(engine);
            if (warnings != null && !warnings.isEmpty()) status += ", warnings=" + warnings;
            return true;
        } catch (Throwable error) {
            status = error.toString();
            engine = 0L;
            return false;
        }
    }

    @Override
    public synchronized void close() {
        if (engine != 0L) {
            CosmicNative.destroyEngine(engine);
            engine = 0L;
            measureCache.clear();
            status = "closed";
        }
    }

    public static final class Result {
        public final BufferedImage image;
        public final List<String> trace;
        public final boolean available;
        public final String status;

        private Result(BufferedImage image, List<String> trace, boolean available,
                       String status) {
            this.image = image;
            this.trace = Collections.unmodifiableList(new ArrayList<>(trace));
            this.available = available;
            this.status = status;
        }
    }

    private static final class Raster {
        final BufferedImage image;
        final float advance;
        final int offsetX;
        final int offsetY;
        final float scale;

        Raster(BufferedImage image, float advance, int offsetX, int offsetY, float scale) {
            this.image = image;
            this.advance = advance;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.scale = scale;
        }
    }
}
