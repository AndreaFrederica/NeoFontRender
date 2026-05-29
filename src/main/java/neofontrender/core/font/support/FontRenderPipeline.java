package neofontrender.core.font.support;

import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/**
 * GL state guard for font quads.
 *
 * <p>The renderer uploads straight-alpha textures. This pipeline makes the
 * blend function explicit, optionally applies a small alpha compensation
 * shader for high-resolution antialiasing edges, then restores the caller's
 * GL state.</p>
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
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO);

        if (NeofontrenderConfig.shaderTextPipeline() && state.previousProgram == 0) {
            int shader = getOrCreateProgram();
            if (shader != 0) {
                GL20.glUseProgram(shader);
                if (textureUniform >= 0) {
                    GL20.glUniform1i(textureUniform, 0);
                }
                if (colorBiasUniform >= 0) {
                    float alphaBoost = alphaBoost(rasterScale);
                    GL20.glUniform4f(colorBiasUniform, 0.0F, 0.0F, 0.0F, alphaBoost);
                }
                state.shaderChanged = true;
            }
        }

        return state;
    }

    private static float alphaBoost(float rasterScale) {
        float brightness = Math.max(0.0F, Math.min(12.0F, NeofontrenderConfig.renderingBrightness()));
        if (brightness <= 0.0F) {
            return 1.0F;
        }
        float scaleCompensation = Math.max(1.0F,
                rasterScale / Math.max(1.0F, FontRenderTuning.currentDrawContext().pixelScale()));
        return Math.min(1.35F, 1.0F + brightness * 0.035F * Math.min(1.4F, scaleCompensation));
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
