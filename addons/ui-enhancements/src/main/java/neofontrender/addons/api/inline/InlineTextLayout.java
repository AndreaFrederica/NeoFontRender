package neofontrender.addons.api.inline;

import net.minecraft.client.gui.FontRenderer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Immutable measurement result shared by drawing, cursor placement and hit testing. */
public final class InlineTextLayout {
    private final String source;
    private final int width;
    private final int height;
    private final List<Run> runs;

    InlineTextLayout(String source, int width, int height, List<Run> runs) {
        this.source = source;
        this.width = width;
        this.height = height;
        this.runs = Collections.unmodifiableList(runs);
    }

    public String source() { return source; }

    public int width() { return width; }

    /** Logical line height required by the tallest text or inline glyph run. */
    public int height() { return height; }

    public boolean hasGlyphs() {
        for (Run run : runs) if (run.glyph != null) return true;
        return false;
    }

    public int draw(FontRenderer font, float x, float y, int argb, boolean shadow) {
        for (Run run : runs) {
            if (run.glyph == null) {
                font.drawString(run.renderText, Math.round(x + run.x),
                        Math.round(y + Math.max(0, height - font.FONT_HEIGHT)), argb, shadow);
            } else {
                int glyphY = Math.round(y + Math.max(0,
                        height - run.glyph.glyph().height(font)));
                run.glyph.glyph().draw(x + run.x, glyphY, argb, shadow);
            }
        }
        return Math.round(x) + width;
    }

    /** Returns the raw UTF-16 source index nearest to the supplied local x coordinate. */
    public int sourceIndexAt(FontRenderer font, int localX) {
        if (localX <= 0) return 0;
        if (localX >= width) return source.length();
        for (Run run : runs) {
            if (localX < run.x || localX > run.x + run.width) continue;
            int relative = Math.max(0, localX - run.x);
            if (run.glyph != null) {
                return relative * 2 < run.width ? run.start : run.end;
            }
            String raw = source.substring(run.start, run.end);
            return run.start + font.trimStringToWidth(raw, relative).length();
        }
        return source.length();
    }

    /** Measures the raw source prefix ending at {@code sourceIndex}. */
    public int widthTo(FontRenderer font, int sourceIndex) {
        int target = Math.max(0, Math.min(source.length(), sourceIndex));
        for (Run run : runs) {
            if (target >= run.end) continue;
            if (target <= run.start) return run.x;
            if (run.glyph != null) return target * 2 < run.start + run.end
                    ? run.x : run.x + run.width;
            return run.x + font.getStringWidth(source.substring(run.start, target));
        }
        return width;
    }

    /** Largest raw source prefix whose visual width does not exceed {@code maximumWidth}. */
    public int sourceIndexFitting(FontRenderer font, int maximumWidth) {
        int limit = Math.max(0, maximumWidth);
        int accepted = 0;
        for (Run run : runs) {
            if (run.x + run.width <= limit) {
                accepted = run.end;
                continue;
            }
            if (run.glyph != null) return accepted;
            int index = run.start;
            while (index < run.end) {
                int next = source.charAt(index) == '\u00a7' && index + 1 < run.end
                        ? index + 2 : index + Character.charCount(Character.codePointAt(source, index));
                if (run.x + font.getStringWidth(source.substring(run.start, next)) > limit) break;
                accepted = next;
                index = next;
            }
            return accepted;
        }
        return source.length();
    }

    @Nullable
    public InlineGlyphHit glyphAt(int localX) {
        return glyphAt(localX, Integer.MAX_VALUE, null);
    }

    /** Returns an image hit only when both coordinates fall inside its actual layout box. */
    @Nullable
    public InlineGlyphHit glyphAt(int localX, int localY, @Nullable FontRenderer font) {
        for (Run run : runs) {
            if (run.glyph != null && localX >= run.x && localX < run.x + run.width) {
                int glyphHeight = font == null ? height : run.glyph.glyph().height(font);
                int glyphY = Math.max(0, height - glyphHeight);
                if (localY != Integer.MAX_VALUE
                        && (localY < glyphY || localY >= glyphY + glyphHeight)) continue;
                return new InlineGlyphHit(run.glyph, run.x, run.width, glyphY, glyphHeight);
            }
        }
        return null;
    }

    static final class Run {
        private final int start;
        private final int end;
        private final int x;
        private final int width;
        private final String renderText;
        private final InlineGlyphMatch glyph;

        private Run(int start, int end, int x, int width, String renderText,
                    InlineGlyphMatch glyph) {
            this.start = start;
            this.end = end;
            this.x = x;
            this.width = width;
            this.renderText = renderText;
            this.glyph = glyph;
        }

        static Run text(int start, int end, int x, int width, String renderText) {
            return new Run(start, end, x, width, renderText, null);
        }

        static Run glyph(InlineGlyphMatch glyph, int x, int width) {
            return new Run(glyph.start(), glyph.end(), x, width, "", glyph);
        }
    }
}
