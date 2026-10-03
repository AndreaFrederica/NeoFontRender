package neofontrender.addons.tooltips;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;

/** Projected model extents, including the origin offset that a width/height alone loses. */
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

    /** Three poses recover each vertex's exact sinusoid over a complete Y-axis turn. */
    static PreviewModelBounds baked(IBakedModel source, Function<Float, Matrix4f> pose, boolean rotating) {
        if (source == null || source.isBuiltInRenderer()) return null;
        Pair<? extends IBakedModel, Matrix4f> perspective =
                source.handlePerspective(ItemCameraTransforms.TransformType.NONE);
        IBakedModel model = perspective.getLeft();
        if (model == null || model.isBuiltInRenderer()) return null;
        Matrix4f[] transforms = {pose.apply(0F), pose.apply(90F), pose.apply(180F)};
        if (perspective.getRight() != null) {
            for (Matrix4f transform : transforms) transform.mul(perspective.getRight());
        }
        Accumulator bounds = new Accumulator();
        addQuads(bounds, model.getQuads(null, null, 0), transforms, rotating);
        for (EnumFacing face : EnumFacing.values()) {
            addQuads(bounds, model.getQuads(null, face, 0), transforms, rotating);
        }
        return bounds.result();
    }

    private static void addQuads(Accumulator bounds, List<BakedQuad> quads,
                                 Matrix4f[] transforms, boolean rotating) {
        for (BakedQuad quad : quads) {
            VertexFormat format = quad.getFormat();
            int offset = -1;
            for (int i = 0; i < format.getElementCount(); i++) {
                if (format.getElement(i).getUsage() == VertexFormatElement.EnumUsage.POSITION) {
                    offset = format.getOffset(i) / 4;
                    break;
                }
            }
            int stride = format.getIntegerSize();
            if (offset < 0 || stride < offset + 3) continue;
            int[] data = quad.getVertexData();
            for (int vertex = offset; vertex + 2 < data.length; vertex += stride) {
                Vector4f point = new Vector4f(Float.intBitsToFloat(data[vertex]) - 0.5F,
                        Float.intBitsToFloat(data[vertex + 1]) - 0.5F,
                        Float.intBitsToFloat(data[vertex + 2]) - 0.5F, 1);
                addVertex(bounds, point, transforms, rotating);
            }
        }
    }

    static void addVertex(Accumulator bounds, Vector4f point, Matrix4f[] transforms, boolean rotating) {
        Vector4f zero = new Vector4f(point);
        transforms[0].transform(zero);
        if (!rotating) {
            bounds.add(zero.x, zero.y, zero.x, zero.y);
            return;
        }
        Vector4f quarter = new Vector4f(point), half = new Vector4f(point);
        transforms[1].transform(quarter);
        transforms[2].transform(half);
        float cx = (zero.x + half.x) * 0.5F, cy = (zero.y + half.y) * 0.5F;
        float rx = (float) Math.hypot(zero.x - cx, quarter.x - cx);
        float ry = (float) Math.hypot(zero.y - cy, quarter.y - cy);
        bounds.add(cx - rx, cy - ry, cx + rx, cy + ry);
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
