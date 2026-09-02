package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.InlineContentMatch;
import neofontrender.api.text.pipeline.InlineContentMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;

import javax.annotation.Nullable;

/** Experimental bounded parser for Markdown constructs that fit a single text line. */
final class MarkdownTextMiddleware implements InlineContentMiddleware {
    private static final int MAX_TOKEN_LENGTH = 512;

    @Override public String id() { return "neofontrender_ui_enhancements:markdown"; }
    @Override public int priority() { return 60; }
    @Override public TextTrigger trigger() { return TextTrigger.exact('*', '_', '~', '`', '['); }
    @Override public boolean isEnabled() { return EmbeddedContentConfig.markdownEnabled(); }

    @Nullable
    @Override
    public InlineContentMatch match(CharSequence source, int sourceIndex) {
        if (isEscaped(source, sourceIndex)) return null;
        char marker = source.charAt(sourceIndex);
        if (marker == '[') return link(source, sourceIndex);
        if (marker == '`') return delimited(source, sourceIndex, "`", "\u00a77");
        if (marker == '~') return startsWith(source, sourceIndex, "~~")
                ? delimited(source, sourceIndex, "~~", "\u00a7m") : null;
        if (marker == '*' || marker == '_') {
            String strong = marker == '*' ? "**" : "__";
            if (startsWith(source, sourceIndex, strong)) {
                return delimited(source, sourceIndex, strong, "\u00a7l");
            }
            return delimited(source, sourceIndex, String.valueOf(marker), "\u00a7o");
        }
        return null;
    }

    @Nullable
    private static InlineContentMatch delimited(CharSequence source, int start,
                                                 String delimiter, String formatting) {
        int contentStart = start + delimiter.length();
        int limit = Math.min(source.length(), start + MAX_TOKEN_LENGTH);
        if (contentStart >= limit || Character.isWhitespace(source.charAt(contentStart))) return null;
        for (int cursor = contentStart; cursor + delimiter.length() <= limit; cursor++) {
            char current = source.charAt(cursor);
            if (current == '\n' || current == '\r') return null;
            if (current != delimiter.charAt(0) || isEscaped(source, cursor)
                    || !startsWith(source, cursor, delimiter)) continue;
            if (cursor == contentStart || Character.isWhitespace(source.charAt(cursor - 1))) return null;
            String visible = unescape(source.subSequence(contentStart, cursor).toString());
            if (visible.isEmpty()) return null;
            int end = cursor + delimiter.length();
            return new InlineContentMatch(start, end,
                    new StyledTextContent(formatting + visible,
                            source.subSequence(start, end).toString()));
        }
        return null;
    }

    @Nullable
    private static InlineContentMatch link(CharSequence source, int start) {
        int limit = Math.min(source.length(), start + MAX_TOKEN_LENGTH);
        int labelEnd = findUnescaped(source, ']', start + 1, limit);
        if (labelEnd <= start + 1 || labelEnd + 2 >= limit || source.charAt(labelEnd + 1) != '(') {
            return null;
        }
        int urlEnd = findUnescaped(source, ')', labelEnd + 2, limit);
        if (urlEnd <= labelEnd + 2) return null;
        String label = unescape(source.subSequence(start + 1, labelEnd).toString());
        String url = source.subSequence(labelEnd + 2, urlEnd).toString().trim();
        if (label.isEmpty() || url.isEmpty() || containsLineBreak(url)) return null;
        return new InlineContentMatch(start, urlEnd + 1,
                new StyledTextContent("\u00a7n" + label, label + " - " + url));
    }

    private static int findUnescaped(CharSequence source, char target, int start, int limit) {
        for (int index = start; index < limit; index++) {
            char current = source.charAt(index);
            if (current == '\n' || current == '\r') return -1;
            if (current == target && !isEscaped(source, index)) return index;
        }
        return -1;
    }

    private static boolean startsWith(CharSequence source, int start, String value) {
        if (start < 0 || start + value.length() > source.length()) return false;
        for (int index = 0; index < value.length(); index++) {
            if (source.charAt(start + index) != value.charAt(index)) return false;
        }
        return true;
    }

    private static boolean isEscaped(CharSequence source, int index) {
        int slashes = 0;
        for (int cursor = index - 1; cursor >= 0 && source.charAt(cursor) == '\\'; cursor--) {
            slashes++;
        }
        return (slashes & 1) != 0;
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private static String unescape(String value) {
        int slash = value.indexOf('\\');
        if (slash < 0) return value;
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '\\' && index + 1 < value.length()) {
                char next = value.charAt(index + 1);
                if (next == '*' || next == '_' || next == '~' || next == '`'
                        || next == '[' || next == ']' || next == '(' || next == ')'
                        || next == '\\') {
                    result.append(next);
                    index++;
                    continue;
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private static final class StyledTextContent implements InlineContent {
        private final String formattedText;
        private final String description;
        private FontRenderer measuredWith;

        StyledTextContent(String formattedText, String description) {
            this.formattedText = formattedText;
            this.description = description;
        }

        @Override public int advance(FontRenderer font) {
            measuredWith = font;
            return font.getStringWidth(formattedText);
        }

        @Override public int height(FontRenderer font) {
            measuredWith = font;
            return font.FONT_HEIGHT;
        }

        @Override public void draw(float x, float y, int argb, boolean shadow) {
            FontRenderer font = measuredWith;
            if (font == null) font = Minecraft.getMinecraft().fontRenderer;
            font.drawString(formattedText, x, y, argb, shadow);
        }

        @Override public String description() { return description; }
    }
}
