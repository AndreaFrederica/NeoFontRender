package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class PreviewModelBoundsTest {
    private static Matrix4f pose(float yaw) {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m00 = -30; matrix.m11 = -30; matrix.m22 = 30;
        Matrix4f rotation = new Matrix4f();
        rotation.rotX((float) Math.toRadians(25)); matrix.mul(rotation);
        rotation.rotY((float) Math.toRadians(yaw)); matrix.mul(rotation);
        rotation.rotZ((float) Math.toRadians(-17)); matrix.mul(rotation);
        matrix.m03 = 18.4F; matrix.m13 = 57;
        return matrix;
    }

    @Test
    void analyticEnvelopeContainsEveryAngleIncludingExtremaBetweenSamples() {
        Vector4f[] points = {new Vector4f(-0.6F, 2.1F, -0.9F, 1),
                new Vector4f(0.8F, 0.1F, 0.4F, 1), new Vector4f(0.1F, 1, 1.2F, 1)};
        PreviewModelBounds.Accumulator accumulator = new PreviewModelBounds.Accumulator();
        Matrix4f[] samples = {pose(0), pose(90), pose(180)};
        for (Vector4f point : points) PreviewModelBounds.addVertex(accumulator, point, samples, true);
        PreviewModelBounds bounds = accumulator.result();
        assertNotNull(bounds);
        for (int step = 0; step < 1440; step++) for (Vector4f original : points) {
            Vector4f point = new Vector4f(original);
            pose(step * 0.25F).transform(point);
            assertTrue(point.x >= bounds.left - 0.001F && point.x <= bounds.right + 0.001F);
            assertTrue(point.y >= bounds.top - 0.001F && point.y <= bounds.bottom + 0.001F);
        }
        assertTrue(bounds.left < 0);
        assertTrue(bounds.top < 0);
    }

    @Test
    void staticPoseDoesNotReserveUnneededFullTurnSpace() {
        Vector4f point = new Vector4f(0, 0, 1, 1);
        Matrix4f identity = new Matrix4f(); identity.setIdentity();
        Matrix4f quarter = new Matrix4f(); quarter.rotY((float) Math.toRadians(90));
        Matrix4f half = new Matrix4f(); half.rotY((float) Math.toRadians(180));
        PreviewModelBounds.Accumulator fixed = new PreviewModelBounds.Accumulator();
        PreviewModelBounds.addVertex(fixed, point, new Matrix4f[]{identity, quarter, half}, false);
        assertEquals(0, fixed.result().left);
        assertEquals(0, fixed.result().right);
    }

    @Test
    void offsetsAndFitKeepDragonSizedOvershootInsideBothMeasuredAndFixedCells() {
        PreviewModelBounds bounds = new PreviewModelBounds(-20.330F, -13.855F, 57.130F, 63).pad(2).includeCell(40, 64);
        assertEquals(83, bounds.width());
        assertEquals(81, bounds.height());
        for (int width : new int[]{40, bounds.width()}) {
            PreviewModelBounds.Placement fit = bounds.fit(width, 64);
            assertTrue(fit.x + bounds.left * fit.scale >= -0.001F);
            assertTrue(fit.y + bounds.top * fit.scale >= -0.001F);
            assertTrue(fit.x + bounds.right * fit.scale <= width + 0.001F);
            assertTrue(fit.y + bounds.bottom * fit.scale <= 64.001F);
        }
    }

    @Test
    void alphaScanConvertsBottomUpRowsAndIncludesLastOpaquePixel() {
        ByteBuffer alpha = ByteBuffer.allocate(8 * 6);
        assertNull(PreviewModelBounds.alpha(alpha, 8, 6));
        alpha.put(1 * 8 + 2, (byte) 255);
        alpha.put(4 * 8 + 6, (byte) 128);
        alpha.put(0, (byte) 1); // Ignore virtually transparent texture noise.
        PreviewModelBounds bounds = PreviewModelBounds.alpha(alpha, 8, 6);
        assertEquals(2, bounds.left); assertEquals(1, bounds.top);
        assertEquals(7, bounds.right); assertEquals(5, bounds.bottom);
    }
}
