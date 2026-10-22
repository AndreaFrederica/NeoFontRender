package neofontrender.api.text.route;

import neofontrender.text.InlineContent;

import java.util.Objects;

/** Logical bounds and source ownership for one atomic inline object. */
public final class TextInlineBounds {
    private final int sourceStart;
    private final int sourceEnd;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final InlineContent content;

    public TextInlineBounds(int sourceStart, int sourceEnd, float x, float y,
                            float width, float height, InlineContent content) {
        this.sourceStart = sourceStart;
        this.sourceEnd = sourceEnd;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.content = Objects.requireNonNull(content, "content");
    }

    public int sourceStart() { return sourceStart; }
    public int sourceEnd() { return sourceEnd; }
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }
    public InlineContent content() { return content; }

    public boolean contains(float localX, float localY) {
        return localX >= x && localX < x + width && localY >= y && localY < y + height;
    }
}
