package neofontrender.core.font.layout;

import neofontrender.core.font.backend.TextRenderResult;

/**
 * Backend-neutral layout object that can later grow beyond bitmap-backed text.
 */
public interface TextLayoutResult {

    String text();

    float advance();

    boolean formatted();

    boolean bold();

    boolean italic();

    boolean shadow();

    TextRenderResult rendered();
}