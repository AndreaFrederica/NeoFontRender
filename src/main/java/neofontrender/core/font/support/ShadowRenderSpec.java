package neofontrender.core.font.support;

import neofontrender.core.config.NeofontrenderConfig;

/** Immutable shadow parameters passed through a render call, including draft-only settings. */
public final class ShadowRenderSpec {
    public final float offsetX;
    public final float offsetY;
    public final float blurRadius;
    public final int color;
    public final String colorMode;
    public final float coloredRatio;
    public final String coloredFunction;
    public final ShadowColorRemapRules colorOverrides;
    public final float opacity;

    private ShadowRenderSpec(float offsetX, float offsetY, float blurRadius, int color,
                             String colorMode, float coloredRatio, String coloredFunction,
                             ShadowColorRemapRules colorOverrides, float opacity) {
        this.offsetX = finite(offsetX, 0.0F);
        this.offsetY = finite(offsetY, 0.0F);
        this.blurRadius = Math.max(0.0F, Math.min(6.0F, finite(blurRadius, 0.0F)));
        this.color = color;
        this.colorMode = ShadowColorPolicy.normalizeMode(colorMode);
        this.coloredRatio = ShadowColorPolicy.clampRatio(coloredRatio);
        this.coloredFunction = ShadowColorPolicy.normalizeColoredFunction(coloredFunction);
        this.colorOverrides = colorOverrides == null ? ShadowColorRemapRules.parse("") : colorOverrides;
        this.opacity = Math.max(0.0F, Math.min(1.0F, finite(opacity, 0.0F)));
    }

    public static ShadowRenderSpec of(float offsetX, float offsetY, float blurRadius, int color,
                                      String colorMode, float coloredRatio, String coloredFunction,
                                      ShadowColorRemapRules colorOverrides, float opacity) {
        return new ShadowRenderSpec(offsetX, offsetY, blurRadius, color, colorMode,
                coloredRatio, coloredFunction, colorOverrides, opacity);
    }

    public static ShadowRenderSpec fromConfig() {
        return of(NeofontrenderConfig.shadowOffsetX(), NeofontrenderConfig.shadowOffsetY(),
                NeofontrenderConfig.shadowBlurRadius(), NeofontrenderConfig.shadowColor(),
                NeofontrenderConfig.shadowColorMode(), NeofontrenderConfig.shadowColoredRatio(),
                NeofontrenderConfig.shadowColoredFunction(),
                NeofontrenderConfig.shadowColorOverrides(), NeofontrenderConfig.shadowOpacity());
    }

    public int profileHash() {
        int hash = Float.floatToIntBits(offsetX);
        hash = 31 * hash + Float.floatToIntBits(offsetY);
        hash = 31 * hash + Float.floatToIntBits(blurRadius);
        hash = 31 * hash + color;
        hash = 31 * hash + colorMode.hashCode();
        hash = 31 * hash + Float.floatToIntBits(coloredRatio);
        hash = 31 * hash + coloredFunction.hashCode();
        hash = 31 * hash + colorOverrides.profileHash();
        return 31 * hash + Float.floatToIntBits(opacity);
    }

    private static float finite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
