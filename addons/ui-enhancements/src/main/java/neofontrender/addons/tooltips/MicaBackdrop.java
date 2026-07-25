package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

/** Captures the game scene before GUI rendering and filters it behind a Mica tooltip. */
final class MicaBackdrop {
    private static final String ROOT = "/assets/neofontrender_ui_enhancements/shaders/";
    private static final int DOWNSAMPLE = 2;
    private static final float GAUSSIAN_SIGMA = 7.0F;
    private static final int GAUSSIAN_RADIUS = 21;
    private static final int FILTER_MARGIN_PX = GAUSSIAN_RADIUS * DOWNSAMPLE + 2;
    private static final GaussianKernel KERNEL = GaussianKernel.create(GAUSSIAN_SIGMA, GAUSSIAN_RADIUS);

    private static int program;
    private static int sceneTexture;
    private static int prefilterTexture;
    private static int horizontalTexture;
    private static int resultTexture;
    private static int prefilterFbo;
    private static int horizontalFbo;
    private static int resultFbo;
    private static int sceneWidth;
    private static int sceneHeight;
    private static int filteredWidth;
    private static int filteredHeight;
    private static boolean unavailable;
    private static boolean sceneValid;

    private MicaBackdrop() {}

    /** Copies the world framebuffer before the current GuiScreen renders any of its UI. */
    static void captureScene() {
        if (unavailable) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        int oldActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            if (sceneTexture == 0) sceneTexture = GL11.glGenTextures();
            if (sceneWidth != mc.displayWidth || sceneHeight != mc.displayHeight) {
                allocateTexture(sceneTexture, mc.displayWidth, mc.displayHeight, GL11.GL_RGBA8);
                sceneWidth = mc.displayWidth;
                sceneHeight = mc.displayHeight;
            }
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0,
                    0, 0, mc.displayWidth, mc.displayHeight);
            sceneValid = true;
        } catch (Throwable t) {
            sceneValid = false;
            unavailable = true;
            TooltipModule.LOGGER.warn("Could not capture the pre-GUI scene for Mica", t);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture);
            GL13.glActiveTexture(oldActiveTexture);
        }
    }

    static void invalidateScene() {
        sceneValid = false;
    }

    static Capture capture(float left, float top, float right, float bottom) {
        if (unavailable || !sceneValid || getOrCreateProgram() == 0) return null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0
                || sceneWidth != mc.displayWidth || sceneHeight != mc.displayHeight) return null;

        ScaledResolution scaled = new ScaledResolution(mc);
        float scaleX = mc.displayWidth / (float) scaled.getScaledWidth();
        float scaleY = mc.displayHeight / (float) scaled.getScaledHeight();
        int x0 = clamp((int) Math.floor(left * scaleX) - FILTER_MARGIN_PX, 0, mc.displayWidth);
        int x1 = clamp((int) Math.ceil(right * scaleX) + FILTER_MARGIN_PX, 0, mc.displayWidth);
        int topPx = clamp((int) Math.floor(top * scaleY) - FILTER_MARGIN_PX, 0, mc.displayHeight);
        int bottomPx = clamp((int) Math.ceil(bottom * scaleY) + FILTER_MARGIN_PX, 0, mc.displayHeight);
        int y0 = mc.displayHeight - bottomPx;
        int width = x1 - x0;
        int height = bottomPx - topPx;
        if (width < 1 || height < 1) return null;

        int downWidth = Math.max(1, (width + DOWNSAMPLE - 1) / DOWNSAMPLE);
        int downHeight = Math.max(1, (height + DOWNSAMPLE - 1) / DOWNSAMPLE);
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int oldFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int oldActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        try {
            ensureTargets(width, height, downWidth, downHeight);

            GL11.glDisable(GL11.GL_BLEND);
            GL20.glUseProgram(program);
            GL20.glUniform1i(GL20.glGetUniformLocation(program, "uTexture"), 0);
            uploadKernel();

            // Decode and average the four source texels before reducing resolution. Sampling the
            // already-minified image directly would alias high-contrast UI/world edges.
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prefilterFbo);
            GL11.glViewport(0, 0, downWidth, downHeight);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneTexture);
            uniform2("uUvOrigin", x0 / (float) sceneWidth, y0 / (float) sceneHeight);
            uniform2("uUvExtent", width / (float) sceneWidth, height / (float) sceneHeight);
            uniform2("uSourceTexel", 1.0F / sceneWidth, 1.0F / sceneHeight);
            uniform1("uPassMode", 0.0F);
            drawFullscreenQuad();

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, horizontalFbo);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prefilterTexture);
            uniform2("uUvOrigin", 0.0F, 0.0F);
            uniform2("uUvExtent", 1.0F, 1.0F);
            uniform2("uTexelStep", 1.0F / downWidth, 0.0F);
            uniform1("uPassMode", 1.0F);
            drawFullscreenQuad();

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, resultFbo);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, horizontalTexture);
            uniform2("uTexelStep", 0.0F, 1.0F / downHeight);
            uniform1("uPassMode", 2.0F);
            drawFullscreenQuad();

            float innerLeft = ((left * scaleX) - x0) / width;
            float innerRight = ((right * scaleX) - x0) / width;
            float innerTop = (mc.displayHeight - top * scaleY - y0) / height;
            float innerBottom = (mc.displayHeight - bottom * scaleY - y0) / height;
            return new Capture(resultTexture, innerLeft, innerTop, innerRight, innerBottom);
        } catch (Throwable t) {
            unavailable = true;
            TooltipModule.LOGGER.warn("Mica backdrop capture failed; using the opaque dark fallback", t);
            return null;
        } finally {
            GL20.glUseProgram(oldProgram);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFbo);
            GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture);
            GL13.glActiveTexture(oldActiveTexture);
            if (blendEnabled) GL11.glEnable(GL11.GL_BLEND);
            else GL11.glDisable(GL11.GL_BLEND);
        }
    }

    private static void ensureTargets(int width, int height, int downWidth, int downHeight) {
        if (prefilterTexture == 0) prefilterTexture = GL11.glGenTextures();
        if (horizontalTexture == 0) horizontalTexture = GL11.glGenTextures();
        if (resultTexture == 0) resultTexture = GL11.glGenTextures();
        if (prefilterFbo == 0) prefilterFbo = GL30.glGenFramebuffers();
        if (horizontalFbo == 0) horizontalFbo = GL30.glGenFramebuffers();
        if (resultFbo == 0) resultFbo = GL30.glGenFramebuffers();

        if (filteredWidth != downWidth || filteredHeight != downHeight) {
            allocateTexture(prefilterTexture, downWidth, downHeight, GL30.GL_RGBA16F);
            allocateTexture(horizontalTexture, downWidth, downHeight, GL30.GL_RGBA16F);
            allocateTexture(resultTexture, downWidth, downHeight, GL11.GL_RGBA8);
            attach(prefilterFbo, prefilterTexture);
            attach(horizontalFbo, horizontalTexture);
            attach(resultFbo, resultTexture);
            filteredWidth = downWidth;
            filteredHeight = downHeight;
        }
    }

    private static void allocateTexture(int texture, int width, int height, int internalFormat) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12Compat.CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12Compat.CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
    }

    private static void attach(int fbo, int texture) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, texture, 0);
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete Mica framebuffer: 0x" + Integer.toHexString(status));
        }
    }

    private static void drawFullscreenQuad() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F); GL11.glVertex2f(-1.0F, -1.0F);
        GL11.glTexCoord2f(1.0F, 0.0F); GL11.glVertex2f(1.0F, -1.0F);
        GL11.glTexCoord2f(1.0F, 1.0F); GL11.glVertex2f(1.0F, 1.0F);
        GL11.glTexCoord2f(0.0F, 1.0F); GL11.glVertex2f(-1.0F, 1.0F);
        GL11.glEnd();
    }

    private static int getOrCreateProgram() {
        if (unavailable) return 0;
        if (program != 0) return program;
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER, read("mica_backdrop.vsh"));
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, read("mica_backdrop.fsh"));
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertex);
            GL20.glAttachShader(program, fragment);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(program, 8192));
            }
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            return program;
        } catch (Throwable t) {
            unavailable = true;
            TooltipModule.LOGGER.warn("Mica backdrop shader is unavailable", t);
            return 0;
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException(GL20.glGetShaderInfoLog(shader, 8192));
        }
        return shader;
    }

    private static String read(String name) throws IOException {
        try (InputStream stream = MicaBackdrop.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IOException("Missing shader " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void uniform1(String name, float value) {
        GL20.glUniform1f(GL20.glGetUniformLocation(program, name), value);
    }

    private static void uniform2(String name, float x, float y) {
        GL20.glUniform2f(GL20.glGetUniformLocation(program, name), x, y);
    }

    private static void uploadKernel() {
        uniform1("uWeight0", KERNEL.centerWeight);
        for (int i = 0; i < KERNEL.pairWeights.length; i++) {
            uniform1("uWeight" + (i + 1), KERNEL.pairWeights[i]);
            uniform1("uOffset" + i, KERNEL.pairOffsets[i]);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class Capture {
        final int texture;
        final float left;
        final float top;
        final float right;
        final float bottom;

        Capture(int texture, float left, float top, float right, float bottom) {
            this.texture = texture;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    /** GL_CLAMP_TO_EDGE is core in 1.2 but absent from the old GL11 constant set. */
    private static final class GL12Compat {
        private static final int CLAMP_TO_EDGE = 0x812F;
    }
}
