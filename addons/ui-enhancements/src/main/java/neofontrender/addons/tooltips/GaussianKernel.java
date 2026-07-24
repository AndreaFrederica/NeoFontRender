package neofontrender.addons.tooltips;

/** Builds a normalized discrete Gaussian and combines adjacent taps for linear sampling. */
final class GaussianKernel {
    final float centerWeight;
    final float[] pairWeights;
    final float[] pairOffsets;
    final float[] discreteWeights;

    private GaussianKernel(float centerWeight, float[] pairWeights,
                           float[] pairOffsets, float[] discreteWeights) {
        this.centerWeight = centerWeight;
        this.pairWeights = pairWeights;
        this.pairOffsets = pairOffsets;
        this.discreteWeights = discreteWeights;
    }

    static GaussianKernel create(float sigma, int radius) {
        if (!(sigma > 0.0F)) throw new IllegalArgumentException("sigma must be positive");
        if (radius < 1) throw new IllegalArgumentException("radius must be positive");

        double[] weights = new double[radius + 1];
        double sum = 0.0D;
        double denominator = 2.0D * sigma * sigma;
        for (int i = 0; i <= radius; i++) {
            weights[i] = Math.exp(-(double) (i * i) / denominator);
            sum += i == 0 ? weights[i] : 2.0D * weights[i];
        }

        float[] discrete = new float[radius + 1];
        for (int i = 0; i <= radius; i++) discrete[i] = (float) (weights[i] / sum);

        int pairCount = (radius + 1) / 2;
        float[] pairWeights = new float[pairCount];
        float[] pairOffsets = new float[pairCount];
        for (int pair = 0; pair < pairCount; pair++) {
            int first = pair * 2 + 1;
            int second = first + 1;
            float firstWeight = discrete[first];
            float secondWeight = second <= radius ? discrete[second] : 0.0F;
            float combined = firstWeight + secondWeight;
            pairWeights[pair] = combined;
            pairOffsets[pair] = second <= radius
                    ? (first * firstWeight + second * secondWeight) / combined
                    : first;
        }
        return new GaussianKernel(discrete[0], pairWeights, pairOffsets, discrete);
    }
}
