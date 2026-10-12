package neofontrender.addons.tooltips;

import java.nio.ByteBuffer;

/**
 * Silhouette extents measured inside a preview cell, including the origin offset that a width
 * and height alone lose. 1.7.10 has no bakeable item model (no {@code IBakedModel} or
 * {@code BakedQuad}), so the extents can only come from a rendered alpha probe
 * ({@link #alpha}); the quad-host path of the 1.12.2 branch has no counterpart here.
 */
final class PreviewModelBounds {
    final float left, top, right, bottom;

    PreviewModelBounds(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    int width() { return Math.max(1, (int) Math.ceil(right - left)); }
    int height() { return Math.max(1, (int) Math.ceil(bottom - top)); }

    PreviewModelBounds pad(float margin) {
        return new PreviewModelBounds(left - margin, top - margin, right + margin, bottom + margin);
    }

    PreviewModelBounds includeCell(int width, int height) {
        // Include the original animation pivot as well as all fully expanded model vertices.
        return new PreviewModelBounds((float) Math.floor(Math.min(0, left)),
                (float) Math.floor(Math.min(0, top)), (float) Math.ceil(Math.max(width, right)),
                (float) Math.ceil(Math.max(height, bottom)));
    }

    Placement fit(int width, int height) {
        float scale = Math.min(1, Math.min(width / (float) width(), height / (float) height()));
        return new Placement(scale, (width - width() * scale) * 0.5F - left * scale,
                (height - height() * scale) * 0.5F - top * scale);
    }

    static final class Placement {
        final float scale, x, y;
        Placement(float scale, float x, float y) { this.scale = scale; this.x = x; this.y = y; }
    }

    /** glReadPixels rows run from the bottom up; return GUI coordinates with exclusive edges. */
    static PreviewModelBounds alpha(ByteBuffer pixels, int width, int height) {
        Accumulator result = new Accumulator();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if ((pixels.get(y * width + x) & 255) > 2) {
                result.add(x, height - y - 1, x + 1, height - y);
            }
        }
        return result.result();
    }

    static final class Accumulator {
        private float left = Float.POSITIVE_INFINITY, top = Float.POSITIVE_INFINITY;
        private float right = Float.NEGATIVE_INFINITY, bottom = Float.NEGATIVE_INFINITY;

        void add(float x0, float y0, float x1, float y1) {
            if (!Float.isFinite(x0) || !Float.isFinite(y0) || !Float.isFinite(x1) || !Float.isFinite(y1)) return;
            left = Math.min(left, x0); top = Math.min(top, y0);
            right = Math.max(right, x1); bottom = Math.max(bottom, y1);
        }

        PreviewModelBounds result() {
            return left <= right && top <= bottom ? new PreviewModelBounds(left, top, right, bottom) : null;
        }
    }
}
