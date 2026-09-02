package neofontrender.api.text.pipeline;

import net.minecraft.client.gui.FontRenderer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/** Immutable measured text containing ordinary runs and atomic inline-content runs. */
public final class TextPipelineLayout {
    private final String source;
    private final int width;
    private final int height;
    private final List<Run> runs;

    TextPipelineLayout(String source, int width, int height, List<Run> runs) {
        this.source = source;
        this.width = width;
        this.height = height;
        this.runs = Collections.unmodifiableList(runs);
    }

    public String source() { return source; }
    public int width() { return width; }
    public int height() { return height; }

    public boolean hasInlineContent() {
        for (Run run : runs) if (run.content != null) return true;
        return false;
    }

    public boolean hasInlineContentInSourceRange(int start, int end) {
        int rangeStart = Math.max(0, Math.min(source.length(), start));
        int rangeEnd = Math.max(rangeStart, Math.min(source.length(), end));
        for (Run run : runs) {
            if (run.content != null && run.start < rangeEnd && run.end > rangeStart) return true;
        }
        return false;
    }

    /** Returns the logical bounds of every atomic object in this line. */
    public List<InlineContentHit> inlineContentBounds(@Nullable FontRenderer font) {
        List<InlineContentHit> result = new ArrayList<>();
        for (Run run : runs) {
            if (run.content == null) continue;
            int contentHeight = font == null ? height : run.content.content().height(font);
            int contentY = Math.max(0, height - contentHeight);
            result.add(new InlineContentHit(run.content, run.x, run.width,
                    contentY, contentHeight));
        }
        return Collections.unmodifiableList(result);
    }

    public int draw(FontRenderer font, float x, float y, int argb, boolean shadow) {
        for (Run run : runs) {
            if (run.content == null) {
                font.drawString(run.renderText, x + run.x,
                        y + Math.max(0, height - font.FONT_HEIGHT), argb, shadow);
            } else {
                int contentY = Math.round(y + Math.max(0,
                        height - run.content.content().height(font)));
                run.content.content().draw(x + run.x, contentY, argb, shadow);
            }
        }
        return Math.round(x) + width;
    }

    public int sourceIndexAt(FontRenderer font, int localX) {
        if (localX <= 0) return 0;
        if (localX >= width) return source.length();
        for (Run run : runs) {
            if (localX < run.x || localX > run.x + run.width) continue;
            int relative = Math.max(0, localX - run.x);
            if (run.content != null) return relative * 2 < run.width ? run.start : run.end;
            String raw = source.substring(run.start, run.end);
            return run.start + font.trimStringToWidth(raw, relative).length();
        }
        return source.length();
    }

    public int widthTo(FontRenderer font, int sourceIndex) {
        int target = Math.max(0, Math.min(source.length(), sourceIndex));
        for (Run run : runs) {
            if (target >= run.end) continue;
            if (target <= run.start) return run.x;
            if (run.content != null) {
                return target * 2 < run.start + run.end ? run.x : run.x + run.width;
            }
            return run.x + font.getStringWidth(source.substring(run.start, target));
        }
        return width;
    }

    public int sourceIndexFitting(FontRenderer font, int maximumWidth) {
        int limit = Math.max(0, maximumWidth);
        int accepted = 0;
        for (Run run : runs) {
            if (run.x + run.width <= limit) {
                accepted = run.end;
                continue;
            }
            if (run.content != null) return accepted;
            String raw = source.substring(run.start, run.end);
            String fitted = font.trimStringToWidth(run.renderText, Math.max(0, limit - run.x));
            int inheritedPrefixLength = run.renderText.length() - raw.length();
            int fittedSourceLength = Math.max(0, fitted.length() - inheritedPrefixLength);
            return run.start + Math.min(raw.length(), fittedSourceLength);
        }
        return source.length();
    }

    public int sourceStartFittingReverse(FontRenderer font, int maximumWidth) {
        int limit = Math.max(0, maximumWidth);
        int accepted = source.length();
        int used = 0;
        for (int runIndex = runs.size() - 1; runIndex >= 0; runIndex--) {
            Run run = runs.get(runIndex);
            if (used + run.width <= limit) {
                used += run.width;
                accepted = run.start;
                continue;
            }
            if (run.content != null) return accepted;
            String raw = source.substring(run.start, run.end);
            String fitted = font.trimStringToWidth(run.renderText,
                    Math.max(0, limit - used), true);
            int fittedSourceLength = Math.min(raw.length(), fitted.length());
            return run.end - fittedSourceLength;
        }
        return 0;
    }

    @Nullable
    public InlineContentHit contentAt(int localX, int localY, @Nullable FontRenderer font) {
        for (Run run : runs) {
            if (run.content == null || localX < run.x || localX >= run.x + run.width) continue;
            int contentHeight = font == null ? height : run.content.content().height(font);
            int contentY = Math.max(0, height - contentHeight);
            if (localY < contentY || localY >= contentY + contentHeight) continue;
            return new InlineContentHit(run.content, run.x, run.width, contentY, contentHeight);
        }
        return null;
    }

    static final class Run {
        final int start;
        final int end;
        final int x;
        final int width;
        final String renderText;
        final InlineContentMatch content;

        private Run(int start, int end, int x, int width,
                    String renderText, InlineContentMatch content) {
            this.start = start;
            this.end = end;
            this.x = x;
            this.width = width;
            this.renderText = renderText;
            this.content = content;
        }

        static Run text(int start, int end, int x, int width, String renderText) {
            return new Run(start, end, x, width, renderText, null);
        }

        static Run content(InlineContentMatch content, int x, int width) {
            return new Run(content.start(), content.end(), x, width, "", content);
        }
    }
}
