package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.pipeline.LayoutText;
import neofontrender.text.layout.CjkLineBreakProvider;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Protocol-aware paragraph wrapper.
 *
 * <p>Color protocols ({@code #RRGGBB[-RRGGBB...]}, Tinkers' PUA triples) are validated by
 * lookahead: a prefix such as {@code #FF634} is ordinary text, while {@code #FF6347} is a
 * zero-width color marker. The measured width of a string is therefore not stable when a
 * caller appends characters one at a time, and any wrapper that cuts the raw source by a
 * width computed from a different prefix can split a marker, leaving continuation lines
 * that render as literal markup.</p>
 *
 * <p>This class removes the ambiguity: the paragraph is parsed once, line breaking happens
 * in the visible coordinate space, and every emitted line is rebuilt with
 * {@link LayoutText#formattedRange(int, int)} so it carries the complete draw state of its
 * first visible character. Callers never concatenate raw slices themselves.</p>
 */
public final class ProtocolTextWrapper {
    /** Surfaces that can opt out of protocol-aware wrapping independently. */
    public enum Scope {
        /** Chat, books and component-based text. */
        CHAT,
        /** Tooltips and their inline content. */
        TOOLTIP,
        /** Pasted sign text. */
        SIGN,
        /** Third-party callers of the legacy string API. */
        OTHER
    }

    private ProtocolTextWrapper() {
    }

    /** Master switch combined with the per-surface scope switch. */
    public static boolean enabled(Scope scope) {
        if (!NeofontrenderConfig.protocolWrapEnabled()) return false;
        switch (scope) {
            case CHAT: return NeofontrenderConfig.protocolWrapChat();
            case TOOLTIP: return NeofontrenderConfig.protocolWrapTooltip();
            case SIGN: return NeofontrenderConfig.protocolWrapSign();
            case OTHER: return NeofontrenderConfig.protocolWrapOther();
            default: return true;
        }
    }

    /** True when the source actually contains a recognized color protocol token. */
    public static boolean hasActiveProtocol(String source) {
        if (source == null || source.isEmpty()) return false;
        return FormattedColorWrapping.prepare(source) != source;
    }

    /**
     * Wraps {@code source} into self-contained formatted lines.
     *
     * @param removeLeadingSpace drop leading spaces after a soft break, matching
     *                           {@code FontRenderer#listFormattedStringToWidth}
     */
    public static List<String> wrap(FontRenderer font, String source, int maxWidth,
                                    boolean removeLeadingSpace) {
        LayoutText text = LayoutText.process(source == null ? "" : source);
        String visible = text.visibleText();
        List<String> lines = new ArrayList<>();
        if (visible.isEmpty()) {
            lines.add("");
            return lines;
        }
        int width = Math.max(1, maxWidth);
        int length = visible.length();
        int cursor = 0;
        while (cursor <= length) {
            int newline = visible.indexOf('\n', cursor);
            int limit = newline < 0 ? length : newline;
            if (cursor >= limit) {
                lines.add("");
                if (newline < 0) break;
                cursor = limit + 1;
                continue;
            }
            int position = cursor;
            while (position < limit) {
                int fittingEnd = fittingEnd(font, text, position, limit, width);
                // The whole remainder fits: never look for a "nicer" break opportunity,
                // otherwise a fitting line would still be split at its last punctuation.
                int lineEnd = fittingEnd >= limit
                        ? limit : chooseBreak(visible, position, fittingEnd);
                if (lineEnd <= position) {
                    lineEnd = nextBoundary(visible, position, limit);
                }
                lines.add(renderLine(text, position, lineEnd));
                int next = lineEnd;
                if (next < limit && visible.charAt(next) == ' ') next++;
                if (removeLeadingSpace) {
                    while (next < limit && visible.charAt(next) == ' ') next++;
                }
                if (next <= position) next = nextBoundary(visible, position, limit);
                position = next;
            }
            if (newline < 0) break;
            cursor = limit + 1;
        }
        // FontRenderer#listFormattedStringToWidth splits the wrapped string, which drops
        // trailing empty lines. Keep that contract so callers can swap the implementation.
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    /** Largest visible boundary whose rebuilt line still fits {@code width}. */
    private static int fittingEnd(FontRenderer font, LayoutText text, int start, int limit,
                                  int width) {
        List<Integer> candidates = boundaries(text.visibleText(), start, limit);
        int low = 0;
        int high = candidates.size() - 1;
        int best = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int end = candidates.get(middle);
            if (font.getStringWidth(renderLine(text, start, end)) <= width) {
                best = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return best >= 0 ? candidates.get(best) : candidates.get(0);
    }

    /**
     * Rebuilds a line from the parsed draw state. A wrapped line is drawn independently, so a
     * leading reset is needed only when the position itself carries color or decoration; plain
     * text then stays byte-identical to its source slice, which matters for callers that store
     * the result (sign lines) instead of drawing it immediately.
     */
    private static String renderLine(LayoutText text, int start, int end) {
        String line = text.formattedRange(start, end);
        if (!text.hasDrawState(start) && line.startsWith("\u00a7r")) {
            return line.substring(2);
        }
        return line;
    }

    /**
     * Prefers a word or CJK opportunity at or before {@code fitEnd}. A space terminates the
     * line before the space itself so the caller can consume it like vanilla does.
     */
    private static int chooseBreak(String visible, int start, int fitEnd) {
        boolean cjk = NeofontrenderConfig.fixCjkLineBreak();
        for (int index = fitEnd; index > start; index--) {
            if (visible.charAt(index - 1) == ' ') {
                return index - 1;
            }
            if (!cjk || index >= visible.length()) continue;
            int previous = visible.codePointBefore(index);
            int next = visible.codePointAt(index);
            if (CjkLineBreakProvider.canBreakBetween(previous, next)) {
                return index;
            }
            // Closing punctuation may end a line even when the next character is a Latin
            // word. Forbidding the colon at line start is not a reason to break inside
            // "速度": the natural break is after the colon, before "Infinity".
            if (CjkLineBreakProvider.isForbiddenAtLineStart(previous)
                    && !CjkLineBreakProvider.isForbiddenAtLineEnd(previous)
                    && !Character.isWhitespace(next)) {
                return index;
            }
        }
        return fitEnd;
    }

    private static List<Integer> boundaries(String visible, int start, int limit) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(visible);
        List<Integer> result = new ArrayList<>();
        for (int boundary = iterator.following(start); boundary != BreakIterator.DONE;
             boundary = iterator.next()) {
            if (boundary > limit) break;
            if (boundary > start) result.add(boundary);
        }
        if (result.isEmpty() || result.get(result.size() - 1) != limit) result.add(limit);
        return result;
    }

    private static int nextBoundary(String visible, int start, int limit) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(visible);
        int boundary = iterator.following(start);
        if (boundary == BreakIterator.DONE || boundary > limit) {
            return Math.min(limit, start + Character.charCount(visible.codePointAt(start)));
        }
        return boundary;
    }
}
