package neofontrender.addons.inline;

import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.InlineContentMatch;
import neofontrender.api.text.pipeline.InlineContentMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;

import javax.annotation.Nullable;

/** Converts LaTeXNH-compatible formula tokens into standard inline glyphs. */
final class LatexContentMiddleware implements InlineContentMiddleware {
    @Override public String id() { return "neofontrender_ui_enhancements:latex"; }
    @Override public int priority() { return 80; }
    @Override public TextTrigger trigger() { return TextTrigger.exact('$'); }
    @Override public boolean isEnabled() { return EmbeddedContentConfig.latexEnabled(); }

    @Nullable
    @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
        LatexTokenParser.Match token = LatexTokenParser.match(source, sourceIndex);
        if (token == null) return null;
        int baseHeight = token.display ? 21 : 14;
        int displayHeight = Math.max(1, Math.round(baseHeight * token.scale));
        InlineContent glyph = RasterGlyphService.INSTANCE.glyph(
                "latex\u0000" + EmbeddedContentConfig.latexFontFamily() + "\u0000"
                        + EmbeddedContentConfig.latexOversample() + "\u0000" + token.formula,
                token.formula,
                displayHeight,
                true,
                EmbeddedContentConfig.latexMatchLineHeight(),
                () -> LatexRasterizer.rasterize(token.formula, EmbeddedContentConfig.latexOversample()));
        return glyph == null ? null : new InlineContentMatch(token.start, token.end, glyph);
    }
}
