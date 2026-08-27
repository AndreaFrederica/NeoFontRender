package neofontrender.core.font.cosmic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import neofontrender.NeoFontRender;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/** Fixed-pipeline-compatible shader for drawing a single-channel SDF texture. */
final class CosmicSdfPipeline {
    private static int program;
    private static int textureUniform = -1;
    private static int colorUniform = -1;
    private static int softnessUniform = -1;
    private static boolean unavailable;

    private CosmicSdfPipeline() {
    }

    static boolean isAvailable() {
        return getOrCreateProgram() != 0;
    }

    static State begin() {
        State state = new State();
        state.capture();
        int shader = getOrCreateProgram();
        if (shader == 0) {
            state.noop = true;
            return state;
        }
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        // Legacy renderers such as TC6 toggle blending through raw GL11 calls and can leave
        // GlStateManager's cache disagreeing with the driver. Keep both layers synchronized.
        GL11.glEnable(GL11.GL_BLEND);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL20.glUseProgram(shader);
        if (textureUniform >= 0) GL20.glUniform1i(textureUniform, 0);
        state.shaderChanged = true;
        return state;
    }

    static void draw(float left, float top, float width, float height, float r, float g, float b,
                     float alpha, float softness) {
        if (colorUniform >= 0) GL20.glUniform4f(colorUniform, r, g, b, alpha);
        if (softnessUniform >= 0) GL20.glUniform1f(softnessUniform, softness);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(left, top, 0).tex(0, 0).endVertex();
        buffer.pos(left, top + height, 0).tex(0, 1).endVertex();
        buffer.pos(left + width, top + height, 0).tex(1, 1).endVertex();
        buffer.pos(left + width, top, 0).tex(1, 0).endVertex();
        tessellator.draw();
    }

    private static int getOrCreateProgram() {
        if (unavailable) return 0;
        if (program != 0) return program;
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER,
                    "#version 110\n" +
                            "void main() { gl_Position = ftransform(); gl_TexCoord[0] = gl_MultiTexCoord0; }\n");
            int fragment = compile(GL20.GL_FRAGMENT_SHADER,
                    "#version 110\n" +
                            "uniform sampler2D sdfTexture;\n" +
                            "uniform vec4 textColor;\n" +
                            "uniform float sdfSoftness;\n" +
                            "void main() {\n" +
                            "  float distance = texture2D(sdfTexture, gl_TexCoord[0].st).r;\n" +
                            "  float smoothing = max(fwidth(distance) * sdfSoftness, 0.015625);\n" +
                            "  float coverage = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);\n" +
                            "  gl_FragColor = vec4(textColor.rgb, textColor.a * coverage);\n" +
                            "}\n");
            int created = GL20.glCreateProgram();
            GL20.glAttachShader(created, vertex);
            GL20.glAttachShader(created, fragment);
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(created, 4096));
            }
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            textureUniform = GL20.glGetUniformLocation(created, "sdfTexture");
            colorUniform = GL20.glGetUniformLocation(created, "textColor");
            softnessUniform = GL20.glGetUniformLocation(created, "sdfSoftness");
            program = created;
            return created;
        } catch (Throwable error) {
            unavailable = true;
            NeoFontRender.LOGGER.warn("Cosmic SDF shader is unavailable; using RGBA glyphs", error);
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

    static final class State implements AutoCloseable {
        private boolean noop;
        private boolean shaderChanged;
        private boolean blendEnabled;
        private boolean alphaEnabled;
        private boolean fogEnabled;
        private boolean textureEnabled;
        private int textureBinding;
        private int srcRgb;
        private int dstRgb;
        private int srcAlpha;
        private int dstAlpha;
        private int previousProgram;

        private void capture() {
            blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
            textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
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
            if (fogEnabled) GlStateManager.enableFog();
            else GlStateManager.disableFog();
            if (textureEnabled) GlStateManager.enableTexture2D();
            else GlStateManager.disableTexture2D();
            GlStateManager.bindTexture(textureBinding);
        }
    }
}
