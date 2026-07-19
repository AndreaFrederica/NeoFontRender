package neofontrender.core.font.layout;

import neofontrender.core.font.backend.TextRenderResult;

public final class CachedTextLayout implements TextLayoutResult {

    private final TextRenderRequest request;
    private final float advance;
    private final TextRenderResult rendered;

    public CachedTextLayout(TextRenderRequest request, float advance, TextRenderResult rendered) {
        this.request = request;
        this.advance = advance;
        this.rendered = rendered;
    }

    @Override
    public String text() {
        return request.text();
    }

    @Override
    public float advance() {
        return advance;
    }

    @Override
    public boolean formatted() {
        return request.formatted();
    }

    @Override
    public boolean bold() {
        return request.bold();
    }

    @Override
    public boolean italic() {
        return request.italic();
    }

    @Override
    public boolean shadow() {
        return request.shadow();
    }

    @Override
    public TextRenderResult rendered() {
        return rendered;
    }
}