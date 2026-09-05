package neofontrender.core.font.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.font.inline.InlineRasterTextRenderResult;
import neofontrender.core.font.support.ScopedFontRenderBypass;
import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.TextStyle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reconstructs vanilla text runs around shared structured inline objects. */
final class VanillaCompatibilityRouteLayout extends AbstractStructuredRouteLayout {
    private final FontRenderer font;
    private final List<Piece> pieces;
    private final float drawOffset;

    VanillaCompatibilityRouteLayout(TextRenderRouteRequest request) {
        this(request, build(request.font(), request.structuredText(), request.argb()),
                inlineGeometry(request, Math.max(1, request.font().FONT_HEIGHT),
                        Math.max(1, request.font().FONT_HEIGHT)));
    }

    private VanillaCompatibilityRouteLayout(TextRenderRouteRequest request,
                                            List<Piece> pieces, InlineGeometry geometry) {
        super(VanillaCompatibilityTextRoute.ID, request, totalAdvance(pieces), geometry.height);
        this.font = request.font();
        this.pieces = pieces;
        this.drawOffset = geometry.drawOffset;
    }

    @Override
    float logicalFontSize() {
        return Math.max(1, font.FONT_HEIGHT);
    }

    @Override
    float measure(StructuredText text) {
        return totalAdvance(build(font, text, request.argb()));
    }

    @Override
    float inlineDrawOffset() {
        return drawOffset;
    }

    @Override
    public void draw(float x, float y) {
        float textY = y + drawOffset;
        for (Piece piece : pieces) {
            float drawX = x + piece.x;
            if (piece.inline != null) {
                float inlineY = y + drawOffset;
                if (request.shadow()) {
                    int shadowColor = vanillaShadowColor(piece.argb);
                    new InlineRasterTextRenderResult(piece.inline.content(),
                            Math.max(1, font.FONT_HEIGHT), shadowColor, true)
                            .draw(drawX + 1.0F, inlineY + 1.0F, 1.0F);
                }
                new InlineRasterTextRenderResult(piece.inline.content(),
                        Math.max(1, font.FONT_HEIGHT), piece.argb, false)
                        .draw(drawX, inlineY, 1.0F);
                continue;
            }
            ScopedFontRenderBypass.call(() -> font.drawString(
                    formatPrefix(piece.style) + piece.text, drawX, textY,
                    piece.argb, request.shadow()));
        }
    }

    private static List<Piece> build(FontRenderer font, StructuredText text, int baseArgb) {
        List<Piece> result = new ArrayList<>();
        List<InlineSpan> inline = new ArrayList<>(text.inlineSpans());
        inline.sort(Comparator.comparingInt(InlineSpan::start));
        int inlineIndex = 0;
        int cursor = 0;
        float x = 0.0F;
        while (cursor < text.plainText().length()) {
            InlineSpan object = inlineIndex < inline.size() ? inline.get(inlineIndex) : null;
            if (object != null && object.start() == cursor) {
                TextStyle style = text.styleAt(cursor);
                int color = styleColor(baseArgb, style);
                float width = inlineWidth(object.content(), Math.max(1, font.FONT_HEIGHT));
                result.add(Piece.inline(object, x, width, color));
                x += width;
                cursor = object.end();
                inlineIndex++;
                continue;
            }
            TextStyle style = text.styleAt(cursor);
            int end = text.plainText().length();
            if (object != null) end = Math.min(end, object.start());
            for (int index = cursor + 1; index < end; index++) {
                if (!style.equals(text.styleAt(index))) { end = index; break; }
            }
            String value = text.plainText().substring(cursor, end);
            String formatted = formatPrefix(style) + value;
            float width = ScopedFontRenderBypass.call(
                    () -> (float) font.getStringWidth(formatted));
            int color = styleColor(baseArgb, style);
            result.add(Piece.text(value, style, x, width, color));
            x += width;
            cursor = end;
        }
        return result;
    }

    private static float totalAdvance(List<Piece> pieces) {
        if (pieces.isEmpty()) return 0.0F;
        Piece last = pieces.get(pieces.size() - 1);
        return last.x + last.width;
    }

    private static int styleColor(int baseArgb, TextStyle style) {
        int normalized = normalizeAlpha(baseArgb);
        return style.hasColorOverride()
                ? (normalized & 0xFF000000) | style.rgb() : normalized;
    }

    private static int normalizeAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    private static int vanillaShadowColor(int color) {
        int normalized = normalizeAlpha(color);
        return normalized & 0xFF000000 | (normalized & 0xFCFCFC) >> 2;
    }

    private static String formatPrefix(TextStyle style) {
        StringBuilder prefix = new StringBuilder(10);
        if (style.obfuscated()) prefix.append("\u00A7k");
        if (style.bold()) prefix.append("\u00A7l");
        if (style.strikethrough()) prefix.append("\u00A7m");
        if (style.underline()) prefix.append("\u00A7n");
        if (style.italic()) prefix.append("\u00A7o");
        return prefix.toString();
    }

    private static final class Piece {
        final String text;
        final TextStyle style;
        final InlineSpan inline;
        final float x;
        final float width;
        final int argb;

        private Piece(String text, TextStyle style, InlineSpan inline,
                      float x, float width, int argb) {
            this.text = text;
            this.style = style;
            this.inline = inline;
            this.x = x;
            this.width = width;
            this.argb = argb;
        }

        static Piece text(String text, TextStyle style, float x, float width, int argb) {
            return new Piece(text, style, null, x, width, argb);
        }

        static Piece inline(InlineSpan inline, float x, float width, int argb) {
            return new Piece("", TextStyle.DEFAULT, inline, x, width, argb);
        }
    }
}
