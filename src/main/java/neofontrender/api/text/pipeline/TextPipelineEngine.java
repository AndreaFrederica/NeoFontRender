package neofontrender.api.text.pipeline;

import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared parser, measurement engine and bounded layout cache for inline middleware. */
public final class TextPipelineEngine {
    private static final int CACHE_LIMIT = 512;
    private static final Map<FontRenderer, LayoutCache> CACHES = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private TextPipelineEngine() {}

    public static TextPipelineLayout layout(FontRenderer font, String source) {
        String text = source == null ? "" : source;
        long revision = TextPipelineApi.revision();
        LayoutCache cache;
        synchronized (CACHES) {
            cache = CACHES.computeIfAbsent(font, ignored -> new LayoutCache());
        }
        TextPipelineLayout cached = cache.get(text, font.FONT_HEIGHT, revision);
        if (cached != null) return cached;

        boolean owner = !Boolean.TRUE.equals(ACTIVE.get());
        if (owner) ACTIVE.set(true);
        try {
            TextPipelineLayout built = build(font, text);
            cache.put(text, font.FONT_HEIGHT, revision, built);
            return built;
        } finally {
            if (owner) ACTIVE.remove();
        }
    }

    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    public static boolean enter() {
        if (isActive()) return false;
        ACTIVE.set(true);
        return true;
    }

    public static void exit() {
        ACTIVE.remove();
    }

    public static void invalidate() {
        TextPipelineApi.invalidate();
        synchronized (CACHES) {
            CACHES.clear();
        }
    }

    public static int width(FontRenderer font, String source) {
        return layout(font, source).width();
    }

    /** Returns the measured pixel height required by a line containing atomic content. */
    public static int height(FontRenderer font, String source) {
        return layout(font, source).height();
    }

    /** Monotonic discriminator for consumers maintaining their own layout caches. */
    public static long layoutGeneration() {
        return TextPipelineApi.revision();
    }

    private static TextPipelineLayout build(FontRenderer font, String text) {
        List<TextPipelineLayout.Run> runs = new ArrayList<>();
        FormattingState formatting = new FormattingState();
        int runStart = 0;
        String runPrefix = "";
        int x = 0;
        int height = Math.max(1, font.FONT_HEIGHT);
        int index = 0;
        while (index < text.length()) {
            if (text.charAt(index) == '\u00a7' && index + 1 < text.length()) {
                formatting.accept(text.charAt(index + 1));
                index += 2;
                continue;
            }
            InlineContentMatch match = TextPipelineApi.matchInline(text, index);
            if (match == null) {
                index++;
                continue;
            }
            if (runStart < index) {
                String raw = text.substring(runStart, index);
                int width = font.getStringWidth(raw);
                runs.add(TextPipelineLayout.Run.text(runStart, index, x, width, runPrefix + raw));
                x += width;
            }
            int width = Math.max(0, match.content().advance(font));
            height = Math.max(height, Math.max(1, match.content().height(font)));
            runs.add(TextPipelineLayout.Run.content(match, x, width));
            x += width;
            for (int scan = index; scan + 1 < match.end(); scan++) {
                if (text.charAt(scan) == '\u00a7') formatting.accept(text.charAt(++scan));
            }
            index = match.end();
            runStart = index;
            runPrefix = formatting.prefix();
        }
        if (runStart < text.length()) {
            String raw = text.substring(runStart);
            int width = font.getStringWidth(raw);
            runs.add(TextPipelineLayout.Run.text(runStart, text.length(), x, width,
                    runPrefix + raw));
            x += width;
        }
        return new TextPipelineLayout(text, x, height, runs);
    }

    private static final class LayoutCache {
        private long revision = Long.MIN_VALUE;
        private final LinkedHashMap<Key, TextPipelineLayout> values =
                new LinkedHashMap<Key, TextPipelineLayout>(CACHE_LIMIT + 1, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Key, TextPipelineLayout> eldest) {
                        return size() > CACHE_LIMIT;
                    }
                };

        synchronized TextPipelineLayout get(String text, int fontHeight, long currentRevision) {
            resetIfChanged(currentRevision);
            return values.get(new Key(text, fontHeight));
        }

        synchronized void put(String text, int fontHeight, long currentRevision,
                              TextPipelineLayout layout) {
            resetIfChanged(currentRevision);
            values.put(new Key(text, fontHeight), layout);
        }

        private void resetIfChanged(long currentRevision) {
            if (revision == currentRevision) return;
            revision = currentRevision;
            values.clear();
        }
    }

    private static final class Key {
        final String text;
        final int fontHeight;

        Key(String text, int fontHeight) {
            this.text = text;
            this.fontHeight = fontHeight;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key)) return false;
            Key key = (Key) other;
            return fontHeight == key.fontHeight && text.equals(key.text);
        }

        @Override public int hashCode() {
            return 31 * text.hashCode() + fontHeight;
        }
    }

    private static final class FormattingState {
        private char color;
        private boolean obfuscated;
        private boolean bold;
        private boolean strike;
        private boolean underline;
        private boolean italic;

        void accept(char rawCode) {
            char code = Character.toLowerCase(rawCode);
            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = code;
                clearStyles();
                return;
            }
            switch (code) {
                case 'k': obfuscated = true; break;
                case 'l': bold = true; break;
                case 'm': strike = true; break;
                case 'n': underline = true; break;
                case 'o': italic = true; break;
                case 'r': color = 0; clearStyles(); break;
                default: break;
            }
        }

        private void clearStyles() {
            obfuscated = bold = strike = underline = italic = false;
        }

        String prefix() {
            StringBuilder result = new StringBuilder(12);
            if (color != 0) result.append('\u00a7').append(color);
            if (obfuscated) result.append("\u00a7k");
            if (bold) result.append("\u00a7l");
            if (strike) result.append("\u00a7m");
            if (underline) result.append("\u00a7n");
            if (italic) result.append("\u00a7o");
            return result.toString();
        }
    }
}
