package neofontrender.core.font.cosmic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import neofontrender.NeoFontRender;
import neofontrender.core.font.support.FramebufferAlphaBlend;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/** Fixed-pipeline-compatible shader for drawing a single-channel SDF texture. */
final class CosmicSdfPipeline {
    /** Keep SDF/atlas text isolated from item and model renderers using Minecraft's global buffer. */
    private static final Tessellator FONT_TESSELLATOR = new Tessellator(0x200000);
    private static int program;
    private static int msdfProgram;
    private static int alphaProgram;
    private static int textureUniform = -1;
    private static int colorUniform = -1;
    private static int softnessUniform = -1;
    private static boolean unavailable;
    private static boolean msdfUnavailable;
    private static boolean alphaUnavailable;
    private static int activeTextureUniform = -1;
    private static int activeColorUniform = -1;
    private static int activeSoftnessUniform = -1;

    private CosmicSdfPipeline() {
    }

    static boolean isAvailable() {
        return getOrCreateProgram(false) != 0;
    }

    static boolean isMsdfAvailable() {
        return getOrCreateProgram(true) != 0;
    }

    static State begin() {
        return begin(false);
    }

    static State begin(boolean msdf) {
        return begin(msdf, false);
    }

    static State beginAlpha() {
        return begin(false, true);
    }

    private static State begin(boolean msdf, boolean alpha) {
        int shader = getOrCreateProgram(msdf, alpha);
        if (shader == 0) {
            State state = new State();
            state.noop = true;
            return state;
        }
        State state = new State();
        state.capture();
        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GlStateManager.disableAlpha();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GlStateManager.disableFog();
        GL11.glDisable(GL11.GL_FOG);
        GlStateManager.enableBlend();
        // Legacy renderers such as TC6 toggle blending through raw GL11 calls and can leave
        // GlStateManager's cache disagreeing with the driver. Keep both layers synchronized.
        GL11.glEnable(GL11.GL_BLEND);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                FramebufferAlphaBlend.SOURCE_FACTOR, FramebufferAlphaBlend.DESTINATION_FACTOR);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                FramebufferAlphaBlend.SOURCE_FACTOR, FramebufferAlphaBlend.DESTINATION_FACTOR);
        GL20.glUseProgram(shader);
        activeTextureUniform = alpha ? alphaTextureUniform : msdf ? msdfTextureUniform : textureUniform;
        activeColorUniform = alpha ? alphaColorUniform : msdf ? msdfColorUniform : colorUniform;
        activeSoftnessUniform = alpha ? -1 : msdf ? msdfSoftnessUniform : softnessUniform;
        if (activeTextureUniform >= 0) GL20.glUniform1i(activeTextureUniform, 0);
        state.shaderChanged = true;
        return state;
    }

    static void draw(float left, float top, float width, float height, float r, float g, float b,
                     float alpha, float softness) {
        drawRegion(left, top, width, height, 0.0F, 0.0F, 1.0F, 1.0F,
                r, g, b, alpha, softness);
    }

    static void draw(boolean msdf, float left, float top, float width, float height,
                     float r, float g, float b, float alpha, float softness) {
        drawRegion(left, top, width, height, 0.0F, 0.0F, 1.0F, 1.0F,
                r, g, b, alpha, softness);
    }

    static void drawAlpha(float left, float top, float width, float height,
                          float r, float g, float b, float alpha) {
        drawRegion(left, top, width, height, 0.0F, 0.0F, 1.0F, 1.0F,
                r, g, b, alpha, 0.0F);
    }

    static void drawRegion(float left, float top, float width, float height,
                           float u1, float v1, float u2, float v2,
                           float r, float g, float b, float alpha, float softness) {
        setUniforms(r, g, b, alpha, softness);
        Tessellator tessellator = FONT_TESSELLATOR;
        BufferBuilder buffer = tessellator.getBuffer();
        beginQuads(buffer);
        quad(buffer, left, top, width, height, u1, v1, u2, v2);
        tessellator.draw();
    }

    static void setUniforms(float r, float g, float b, float alpha, float softness) {
        if (activeColorUniform >= 0) GL20.glUniform4f(activeColorUniform, r, g, b, alpha);
        if (activeSoftnessUniform >= 0) GL20.glUniform1f(activeSoftnessUniform, softness);
    }

    static BufferBuilder beginBatch() {
        BufferBuilder buffer = FONT_TESSELLATOR.getBuffer();
        beginQuads(buffer);
        return buffer;
    }

    static void addQuad(BufferBuilder buffer, float left, float top, float width, float height,
                        float u1, float v1, float u2, float v2) {
        quad(buffer, left, top, width, height, u1, v1, u2, v2);
    }

    static void endBatch() {
        FONT_TESSELLATOR.draw();
    }

    private static void beginQuads(BufferBuilder buffer) {
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
    }

    private static void quad(BufferBuilder buffer, float left, float top, float width, float height,
                             float u1, float v1, float u2, float v2) {
        buffer.pos(left, top, 0).tex(u1, v1).endVertex();
        buffer.pos(left, top + height, 0).tex(u1, v2).endVertex();
        buffer.pos(left + width, top + height, 0).tex(u2, v2).endVertex();
        buffer.pos(left + width, top, 0).tex(u2, v1).endVertex();
    }

    private static int getOrCreateProgram() { return getOrCreateProgram(false, false); }

    private static int getOrCreateProgram(boolean msdf) {
        return getOrCreateProgram(msdf, false);
    }

    private static int getOrCreateProgram(boolean msdf, boolean alpha) {
        if (alpha ? alphaUnavailable : msdf ? msdfUnavailable : unavailable) return 0;
        if (alpha ? alphaProgram != 0 : msdf ? msdfProgram != 0 : program != 0) {
            return alpha ? alphaProgram : msdf ? msdfProgram : program;
        }
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER,
                    "#version 110\n" +
                            "void main() { gl_Position = ftransform(); gl_TexCoord[0] = gl_MultiTexCoord0; }\n");
            String fragmentSource = alpha
                    ? "#version 110\n" +
                    "uniform sampler2D sdfTexture;\n" +
                    "uniform vec4 textColor;\n" +
                    "void main() {\n" +
                    "  float coverage = texture2D(sdfTexture, gl_TexCoord[0].st).r;\n" +
                    "  gl_FragColor = vec4(textColor.rgb, textColor.a * coverage);\n" +
                    "}\n"
                    : msdf
                    ? "#version 110\n" +
                    "uniform sampler2D sdfTexture;\n" +
                    "uniform vec4 textColor;\n" +
                    "uniform float sdfSoftness;\n" +
                    "float median3(float a, float b, float c) { return max(min(a,b), min(max(a,b),c)); }\n" +
                    "void main() {\n" +
                    "  vec3 sample = texture2D(sdfTexture, gl_TexCoord[0].st).rgb;\n" +
                    "  float distance = median3(sample.r, sample.g, sample.b);\n" +
                    "  float smoothing = max(fwidth(distance) * sdfSoftness, 0.015625);\n" +
                    "  float coverage = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);\n" +
                    "  gl_FragColor = vec4(textColor.rgb, textColor.a * coverage);\n" +
                    "}\n"
                    :
                    "#version 110\n" +
                            "uniform sampler2D sdfTexture;\n" +
                            "uniform vec4 textColor;\n" +
                            "uniform float sdfSoftness;\n" +
                            "void main() {\n" +
                            "  float distance = texture2D(sdfTexture, gl_TexCoord[0].st).r;\n" +
                            "  float smoothing = max(fwidth(distance) * sdfSoftness, 0.015625);\n" +
                            "  float coverage = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);\n" +
                            "  gl_FragColor = vec4(textColor.rgb, textColor.a * coverage);\n" +
                            "}\n";
            int created = GL20.glCreateProgram();
            GL20.glAttachShader(created, vertex);
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, fragmentSource);
            GL20.glAttachShader(created, fragment);
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(created, 4096));
            }
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            int texture = GL20.glGetUniformLocation(created, "sdfTexture");
            int color = GL20.glGetUniformLocation(created, "textColor");
            int softness = GL20.glGetUniformLocation(created, "sdfSoftness");
            if (alpha) {
                alphaTextureUniform = texture; alphaColorUniform = color; alphaProgram = created;
            } else if (msdf) {
                msdfTextureUniform = texture; msdfColorUniform = color; msdfSoftnessUniform = softness; msdfProgram = created;
            } else {
                textureUniform = texture; colorUniform = color; softnessUniform = softness; program = created;
            }
            return created;
        } catch (Throwable error) {
            if (alpha) alphaUnavailable = true; else if (msdf) msdfUnavailable = true; else unavailable = true;
            NeoFontRender.LOGGER.warn("Cosmic SDF shader is unavailable; using RGBA glyphs", error);
            return 0;
        }
    }

    private static int msdfTextureUniform = -1;
    private static int msdfColorUniform = -1;
    private static int msdfSoftnessUniform = -1;
    private static int alphaTextureUniform = -1;
    private static int alphaColorUniform = -1;

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

    static final class State implements AutoCloseable {
        private boolean noop;
        private boolean shaderChanged;
        private boolean blendEnabled;
        private boolean alphaEnabled;
        private boolean fogEnabled;
        private int activeTexture;
        private boolean activeTextureEnabled;
        private int activeTextureBinding;
        private boolean texture0Enabled;
        private int texture0Binding;
        private int srcRgb;
        private int dstRgb;
        private int srcAlpha;
        private int dstAlpha;
        private int previousProgram;

        private void capture() {
            blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            activeTextureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            activeTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            selectTextureUnit(GL13.GL_TEXTURE0);
            texture0Enabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            texture0Binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            previousProgram = GL11.glGetInteger(0x8B8D);
        }

        boolean isNoop() {
            return noop;
        }

        @Override
        public void close() {
            if (noop) return;
            if (shaderChanged) GL20.glUseProgram(previousProgram);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blendEnabled) {
                GlStateManager.enableBlend();
                GL11.glEnable(GL11.GL_BLEND);
            } else {
                GlStateManager.disableBlend();
                GL11.glDisable(GL11.GL_BLEND);
            }
            if (alphaEnabled) GlStateManager.enableAlpha();
            else GlStateManager.disableAlpha();
            if (alphaEnabled) GL11.glEnable(GL11.GL_ALPHA_TEST);
            else GL11.glDisable(GL11.GL_ALPHA_TEST);
            if (fogEnabled) GlStateManager.enableFog();
            else GlStateManager.disableFog();
            if (fogEnabled) GL11.glEnable(GL11.GL_FOG);
            else GL11.glDisable(GL11.GL_FOG);
            restoreTextureUnit(GL13.GL_TEXTURE0, texture0Enabled, texture0Binding);
            if (activeTexture != GL13.GL_TEXTURE0) {
                restoreTextureUnit(activeTexture, activeTextureEnabled, activeTextureBinding);
            }
            selectTextureUnit(activeTexture);
        }

        private static void restoreTextureUnit(int unit, boolean enabled, int binding) {
            selectTextureUnit(unit);
            if (enabled) GlStateManager.enableTexture2D();
            else GlStateManager.disableTexture2D();
            if (enabled) GL11.glEnable(GL11.GL_TEXTURE_2D);
            else GL11.glDisable(GL11.GL_TEXTURE_2D);
            GlStateManager.bindTexture(binding);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
        }

        private static void selectTextureUnit(int unit) {
            GlStateManager.setActiveTexture(unit);
            GL13.glActiveTexture(unit);
        }
    }
}
