package neofontrender.core.font.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontRenderTuningProjectionTest {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    @Test
    void zoomedPerspectiveSelectsLargerWorldTextScale() {
        float localScale = 0.025F;
        float distance = 10.0F;
        float normal = FontRenderTuning.measureProjectedPixelScale(
                modelView(localScale, distance), perspective(70.0F), WIDTH, HEIGHT);
        float zoomed = FontRenderTuning.measureProjectedPixelScale(
                modelView(localScale, distance), perspective(70.0F / 4.0F), WIDTH, HEIGHT);

        assertEquals(expectedPixelScale(localScale, distance, 70.0F), normal, 0.0001F);
        assertEquals(expectedPixelScale(localScale, distance, 70.0F / 4.0F), zoomed, 0.0001F);
        assertTrue(zoomed > normal * 4.0F,
                "FOV division is angular, so a nominal 4x zoom is slightly above 4x in device space");
    }

    @Test
    void rejectsProjectionAtCameraOrigin() {
        assertTrue(Float.isNaN(FontRenderTuning.measureProjectedPixelScale(
                modelView(1.0F, 0.0F), perspective(70.0F), WIDTH, HEIGHT)));
    }

    private static float[] modelView(float scale, float distance) {
        float[] matrix = identity();
        matrix[0] = scale;
        matrix[5] = scale;
        matrix[10] = scale;
        matrix[14] = -distance;
        return matrix;
    }

    private static float[] perspective(float fovDegrees) {
        float near = 0.05F;
        float far = 1000.0F;
        float aspect = (float) WIDTH / HEIGHT;
        float focalLength = (float) (1.0D / Math.tan(Math.toRadians(fovDegrees) * 0.5D));
        float[] matrix = new float[16];
        matrix[0] = focalLength / aspect;
        matrix[5] = focalLength;
        matrix[10] = (far + near) / (near - far);
        matrix[11] = -1.0F;
        matrix[14] = 2.0F * far * near / (near - far);
        return matrix;
    }

    private static float expectedPixelScale(float localScale, float distance, float fovDegrees) {
        float focalLength = (float) (1.0D / Math.tan(Math.toRadians(fovDegrees) * 0.5D));
        return localScale * focalLength * HEIGHT / (2.0F * distance);
    }

    private static float[] identity() {
        float[] matrix = new float[16];
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1.0F;
        return matrix;
    }
}
