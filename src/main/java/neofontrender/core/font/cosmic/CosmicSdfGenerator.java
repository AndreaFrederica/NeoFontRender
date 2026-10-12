package neofontrender.core.font.cosmic;

/** Small CPU mask-to-SDF converter used only for opt-in monochrome glyphs. */
final class CosmicSdfGenerator {
    private static final float INF = 1.0E6F;
    private static final float DIAGONAL = 1.41421356F;

    private CosmicSdfGenerator() {
    }

    static byte[] generate(int[] pixels, int width, int height, int range) {
        if (pixels == null || width <= 0 || height <= 0) {
            return new byte[0];
        }
        int distanceRange = Math.max(1, range);
        float[] inside = distanceTo(pixels, width, height, true);
        float[] outside = distanceTo(pixels, width, height, false);
        byte[] result = new byte[width * height];
        for (int index = 0; index < result.length; index++) {
            int alpha = (pixels[index] >>> 24) & 0xFF;
            boolean covered = alpha >= 128;
            float signed;
            if (alpha > 0 && alpha < 255) {
                // Preserve the original antialias coverage at the contour. A hard 128 threshold
                // makes small glyphs visibly heavier because every partial edge texel becomes a
                // full inside/outside decision before the shader sees it.
                signed = alpha / 255.0F - 0.5F;
            } else {
                signed = covered ? outside[index] : -inside[index];
            }
            float normalized = 0.5F + signed / (2.0F * distanceRange);
            result[index] = (byte) Math.round(Math.max(0.0F, Math.min(1.0F, normalized)) * 255.0F);
        }
        return result;
    }

    private static float[] distanceTo(int[] pixels, int width, int height, boolean coveredTarget) {
        float[] distance = new float[width * height];
        for (int index = 0; index < distance.length; index++) {
            boolean covered = ((pixels[index] >>> 24) & 0xFF) >= 128;
            distance[index] = covered == coveredTarget ? 0.0F : INF;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                float value = distance[index];
                if (x > 0) value = Math.min(value, distance[index - 1] + 1.0F);
                if (y > 0) value = Math.min(value, distance[index - width] + 1.0F);
                if (x > 0 && y > 0) value = Math.min(value, distance[index - width - 1] + DIAGONAL);
                if (x + 1 < width && y > 0) value = Math.min(value, distance[index - width + 1] + DIAGONAL);
                distance[index] = value;
            }
        }
        for (int y = height - 1; y >= 0; y--) {
            for (int x = width - 1; x >= 0; x--) {
                int index = y * width + x;
                float value = distance[index];
                if (x + 1 < width) value = Math.min(value, distance[index + 1] + 1.0F);
                if (y + 1 < height) value = Math.min(value, distance[index + width] + 1.0F);
                if (x + 1 < width && y + 1 < height) value = Math.min(value, distance[index + width + 1] + DIAGONAL);
                if (x > 0 && y + 1 < height) value = Math.min(value, distance[index + width - 1] + DIAGONAL);
                distance[index] = value;
            }
        }
        return distance;
    }
}
