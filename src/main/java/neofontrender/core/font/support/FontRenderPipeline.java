package neofontrender.core.font.support;

import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/**
 * GL state guard for font quads with SmoothFont-style brightness correction.
 *
 * <p>The renderer uploads straight-alpha or premultiplied-alpha textures.
 * This pipeline applies a colorBias uniform that offsets RGB and scales Alpha
 * to compensate for darkening introduced by GL_LINEAR bilinear filtering.</p>
 */
public final class FontRenderPipeline {

    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static int program;
    private static int colorBiasUniform = -1;
    private static int textureUniform = -1;
    private static boolean shaderUnavailable;

    private FontRenderPipeline() {
    }

    public static State begin(float rasterScale) {
        if (!NeofontrenderConfig.isLoaded() || !NeofontrenderConfig.enhancedTextPipeline()) {
            return State.NOOP;
        }

        State state = new State();
        state.capture();

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();

        boolean premultiplied = NeofontrenderConfig.enablePremultipliedAlpha();
        if (premultiplied) {
            GlStateManager.tryBlendFuncSeparate(
                    GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ZERO);
        } else {
            GlStateManager.tryBlendFuncSeparate(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ZERO);
        }

        if (NeofontrenderConfig.shaderTextPipeline() && state.previousProgram == 0) {
            int shader = getOrCreateProgram();
            if (shader != 0) {
                GL20.glUseProgram(shader);
                if (textureUniform >= 0) {
                    GL20.glUniform1i(textureUniform, 0);
                }
                if (colorBiasUniform >= 0) {
                    float[] bias = computeColorBias(rasterScale, premultiplied);
                    GL20.glUniform4f(colorBiasUniform, bias[0], bias[1], bias[2], bias[3]);
                }
                state.shaderChanged = true;
            }
        }

        return state;
    }

    /**
     * Computes the colorBias uniform matching SmoothFont's formula.
     *
     * @param rasterScale     current raster scale
     * @param premultiplied   whether premultiplied alpha mode is active
     * @return float[4] = {r, g, b, a}
     */
    public static float[] computeColorBias(float rasterScale, boolean premultiplied) {
        float brightness;
        float boundaryScaleFactor;

        if (NeofontrenderConfig.renderingBrightnessAuto() && FontBrightnessEstimator.isAutoDetected()) {
            brightness = FontBrightnessEstimator.getBrightness();
            boundaryScaleFactor = FontBrightnessEstimator.getBoundaryScaleFactor();
        } else {
            brightness = Math.max(0.0F, Math.min(20.0F, NeofontrenderConfig.renderingBrightness()));
            boundaryScaleFactor = FontBrightnessEstimator.getBoundaryScaleFactor();
        }

        if (brightness <= 0.0F) {
            return premultiplied ? new float[]{0.0F, 0.0F, 0.0F, 1.0F} : new float[]{0.0F, 0.0F, 0.0F, 1.0F};
        }

        float scale = FontRenderTuning.currentDrawContext().pixelScale();
        float factor;
        if (scale < 1.5f) {
            factor = 7.0f;
        } else {
            factor = scale < boundaryScaleFactor
                    ? 20.0f - 13.0f / (boundaryScaleFactor - 1.5f) * (boundaryScaleFactor - scale)
                    : 20.0f;
        }

        float alpha = 1.0f + brightness / factor;
        float rgb;
        if (premultiplied) {
            rgb = (alpha - 1.0f) / 2.0f;
        } else {
            rgb = 0.0f;
        }

        return new float[]{rgb, rgb, rgb, alpha};
    }

    private static int getOrCreateProgram() {
        if (shaderUnavailable) {
            return 0;
        }
        if (program != 0) {
            return program;
        }
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER,
                    "#version 110\n" +
                    "void main() {\n" +
                    "    gl_Position = ftransform();\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_FrontColor = gl_Color;\n" +
                    "}\n");
            int fragment = compile(GL20.GL_FRAGMENT_SHADER,
                    "#version 110\n" +
                    "uniform sampler2D fontTexture;\n" +
                    "uniform vec4 colorBias;\n" +
                    "void main() {\n" +
                    "    vec4 sampled = texture2D(fontTexture, gl_TexCoord[0].st);\n" +
                    "    sampled.r = clamp(sampled.r + colorBias.r, 0.0, 1.0);\n" +
                    "    sampled.g = clamp(sampled.g + colorBias.g, 0.0, 1.0);\n" +
                    "    sampled.b = clamp(sampled.b + colorBias.b, 0.0, 1.0);\n" +
                    "    sampled.a = clamp(sampled.a * colorBias.a, 0.0, 1.0);\n" +
                    "    gl_FragColor = sampled * gl_Color;\n" +
                    "}\n");

            int created = GL20.glCreateProgram();
            GL20.glAttachShader(created, vertex);
            GL20.glAttachShader(created, fragment);
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(created, 4096));
            }
            GL20.glDetachShader(created, vertex);
            GL20.glDetachShader(created, fragment);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            colorBiasUniform = GL20.glGetUniformLocation(created, "colorBias");
            textureUniform = GL20.glGetUniformLocation(created, "fontTexture");
            program = created;
            return program;
        } catch (Throwable t) {
            shaderUnavailable = true;
            NeoFontRender.LOGGER.warn("Font shader pipeline is unavailable; using fixed-function blending", t);
            return 0;
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 4096);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    public static final class State implements AutoCloseable {
        private static final State NOOP = new State(true);

        private final boolean noop;
        private boolean blendEnabled;
        private int srcRgb;
        private int dstRgb;
        private int srcAlpha;
        private int dstAlpha;
        private int previousProgram;
        private boolean shaderChanged;

        private State() {
            this(false);
        }

        private State(boolean noop) {
            this.noop = noop;
        }

        private void capture() {
            this.blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            this.srcRgb = getInteger(GL14.GL_BLEND_SRC_RGB, GL11.GL_SRC_ALPHA);
            this.dstRgb = getInteger(GL14.GL_BLEND_DST_RGB, GL11.GL_ONE_MINUS_SRC_ALPHA);
            this.srcAlpha = getInteger(GL14.GL_BLEND_SRC_ALPHA, GL11.GL_ONE);
            this.dstAlpha = getInteger(GL14.GL_BLEND_DST_ALPHA, GL11.GL_ZERO);
            this.previousProgram = getInteger(GL_CURRENT_PROGRAM, 0);
        }

        @Override
        public void close() {
            if (noop) {
                return;
            }
            if (shaderChanged) {
                GL20.glUseProgram(previousProgram);
            }
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (!blendEnabled) {
                GlStateManager.disableBlend();
            }
        }

        private static int getInteger(int key, int fallback) {
            try {
                return GL11.glGetInteger(key);
            } catch (RuntimeException | LinkageError ignored) {
                return fallback;
            }
        }
    }
}
