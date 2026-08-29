package neofontrender.addons.outlines;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.AxisAlignedBB;
import neofontrender.addons.ui.NfrUiEnhancements;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Draws constant-framebuffer-pixel block outlines on Minecraft's active GL context.
 * Arc3D Core supplies color/math primitives; geometry stays on the host context so it
 * can share Minecraft's camera matrices and depth buffer on every supported port.
 */
public final class BlockOutlineRenderer {
    private static final String SHADER_ROOT = "/assets/neofontrender_ui_enhancements/shaders/";
    private static final int[][] EDGES = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0},
            {4, 5}, {5, 7}, {7, 6}, {6, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final float[][] LOCAL_CORNERS = {
            {0.0F, 0.0F, 0.0F}, {1.0F, 0.0F, 0.0F},
            {0.0F, 1.0F, 0.0F}, {1.0F, 1.0F, 0.0F},
            {0.0F, 0.0F, 1.0F}, {1.0F, 0.0F, 1.0F},
            {0.0F, 1.0F, 1.0F}, {1.0F, 1.0F, 1.0F}
    };
    private static int program;
    private static boolean shaderUnavailable;
    private static float nativeMinimum = Float.NaN;
    private static float nativeMaximum = Float.NaN;

    private BlockOutlineRenderer() {}

    public static void draw(AxisAlignedBB source, BlockOutlineResolver.ResolvedOutline outline) {
        if (source == null || outline == null) return;
        AxisAlignedBB box = source.grow(BlockOutlineConfig.expansion);
        GlState state = new GlState();
        try {
            prepareState();
            float pulse = pulseAlpha();
            if (BlockOutlineConfig.DEPTH_XRAY.equals(BlockOutlineConfig.depthMode)) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_GREATER);
                drawPass(box, outline, pulse * BlockOutlineConfig.xrayHiddenOpacity);
                GL11.glDepthFunc(state.depthFunction);
                drawPass(box, outline, pulse);
            } else if (BlockOutlineConfig.DEPTH_ALWAYS.equals(BlockOutlineConfig.depthMode)) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                drawPass(box, outline, pulse);
            } else {
                if (state.depthEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
                else GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(state.depthFunction);
                drawPass(box, outline, pulse);
            }
        } catch (Throwable throwable) {
            NfrUiEnhancements.LOGGER.warn("Advanced block-outline rendering failed; using the native outline", throwable);
            GL20.glUseProgram(state.program);
            drawNative(box, outline, 1.0F);
        } finally {
            state.restore();
        }
    }

    static float nativeMinimumWidth() {
        queryNativeWidthRange();
        return Float.isFinite(nativeMinimum) ? nativeMinimum : 1.0F;
    }

    static float nativeMaximumWidth() {
        queryNativeWidthRange();
        return Float.isFinite(nativeMaximum) ? nativeMaximum : 1.0F;
    }

    private static void prepareState() {
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        applyConfiguredBlend();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_SMOOTH);
    }

    private static void applyConfiguredBlend() {
        if (BlockOutlineConfig.BLEND_ADDITIVE.equals(BlockOutlineConfig.blendMode)) {
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        } else {
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static void applyGlowBlend() {
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
    }

    private static void drawPass(AxisAlignedBB box, BlockOutlineResolver.ResolvedOutline outline,
                                 float alphaMultiplier) {
        float multiplier = MathUtil.clamp(alphaMultiplier, 0.0F, 1.0F);
        if (multiplier <= 0.0F) return;
        if (BlockOutlineConfig.fillEnabled && BlockOutlineConfig.fillOpacity > 0.0F) {
            RenderGlobal.renderFilledBox(box, outline.red, outline.green, outline.blue,
                    outline.alpha * multiplier * BlockOutlineConfig.fillOpacity);
        }
        if (BlockOutlineConfig.MODE_GEOMETRY.equals(BlockOutlineConfig.renderMode)
                && drawGeometry(box, outline, multiplier)) return;
        drawNative(box, outline, multiplier);
    }

    private static boolean drawGeometry(AxisAlignedBB box, BlockOutlineResolver.ResolvedOutline outline,
                                        float alphaMultiplier) {
        int shader = getOrCreateProgram();
        if (shader == 0) return false;

        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        int viewportX = viewport.get(0);
        int viewportY = viewport.get(1);
        int viewportWidth = viewport.get(2);
        int viewportHeight = viewport.get(3);
        if (viewportWidth <= 0 || viewportHeight <= 0) return false;

        double[][] world = corners(box);
        ClipPoint[] clip = new ClipPoint[world.length];
        for (int i = 0; i < world.length; i++) {
            clip[i] = transform(projection, transform(modelView,
                    new ClipPoint(world[i][0], world[i][1], world[i][2], 1.0D)));
        }

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        boolean previousPolygonOffset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        float previousPolygonOffsetFactor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
        float previousPolygonOffsetUnits = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
        try {
            // The screen-space quad grows across the selected block's face while retaining the
            // edge's depth. A small polygon offset prevents that face from self-occluding the
            // inward half of a thick stroke without disabling normal world-depth occlusion.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0F, -10.0F);
            GL20.glUseProgram(shader);
            uniform1(shader, "uHalfWidth", MathUtil.clamp(outline.lineWidth, 0.5F, 64.0F) * 0.5F);
            uniform1(shader, "uFeather", BlockOutlineConfig.antialias
                    ? MathUtil.clamp(BlockOutlineConfig.antialiasWidth, 0.25F, 4.0F) : 0.0F);
            uniform1(shader, "uPattern", patternValue());
            uniform1(shader, "uRoundCaps", BlockOutlineConfig.CAP_ROUND.equals(BlockOutlineConfig.cap) ? 1.0F : 0.0F);
            uniform1(shader, "uDashLength", Math.max(1.0F, BlockOutlineConfig.dashLength));
            uniform1(shader, "uDashGap", Math.max(0.5F, BlockOutlineConfig.dashGap));
            uniform1(shader, "uRainbow", BlockOutlineConfig.rainbowEnabled ? 1.0F : 0.0F);
            uniform1(shader, "uRainbowPhase", rainbowCyclePhase(System.currentTimeMillis(),
                    BlockOutlineConfig.rainbowCycleMillis));
            uniform1(shader, "uBrightness", brightness());
            float rainbowDensity = MathUtil.clamp(BlockOutlineConfig.rainbowDensity, 0.25F, 4.0F);
            float glowRadius = BlockOutlineConfig.glowEnabled
                    ? MathUtil.clamp(BlockOutlineConfig.glowRadius, 0.5F, 24.0F) : 0.0F;
            float glowIntensity = MathUtil.clamp(BlockOutlineConfig.glowIntensity, 0.0F, 2.0F);
            uniform1(shader, "uGlowRadius", glowRadius);
            uniform1(shader, "uGlowIntensity", glowIntensity);
            uniform1(shader, "uGlowFalloff", MathUtil.clamp(BlockOutlineConfig.glowFalloff, 0.5F, 4.0F));
            GL20.glUniform4f(GL20.glGetUniformLocation(shader, "uColor"),
                    Color.red(outline.argb) / 255.0F * brightness(),
                    Color.green(outline.argb) / 255.0F * brightness(),
                    Color.blue(outline.argb) / 255.0F * brightness(),
                    Color.alpha(outline.argb) / 255.0F * lineAlpha(alphaMultiplier));

            float feather = BlockOutlineConfig.antialias ? BlockOutlineConfig.antialiasWidth : 0.0F;
            if (glowRadius > 0.0F && glowIntensity > 0.0F) {
                // Keep the additive halo separate so the configured blend mode still controls the crisp core.
                applyGlowBlend();
                uniform1(shader, "uGlowPass", 1.0F);
                drawEdges(shader, clip, viewportX, viewportY, viewportWidth, viewportHeight,
                        outline.lineWidth, feather, glowRadius, rainbowDensity);
                applyConfiguredBlend();
            }
            uniform1(shader, "uGlowPass", 0.0F);
            drawEdges(shader, clip, viewportX, viewportY, viewportWidth, viewportHeight,
                    outline.lineWidth, feather, glowRadius, rainbowDensity);
            return true;
        } finally {
            GL20.glUseProgram(previousProgram);
            GL11.glPolygonOffset(previousPolygonOffsetFactor, previousPolygonOffsetUnits);
            if (previousPolygonOffset) GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            else GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }

    private static void drawEdges(int shader, ClipPoint[] clip,
                                  int viewportX, int viewportY, int viewportWidth, int viewportHeight,
                                  float width, float feather, float glowRadius, float rainbowDensity) {
        for (int[] edge : EDGES) {
            drawEdge(shader, edge[0], edge[1], clip[edge[0]], clip[edge[1]], viewportX, viewportY,
                    viewportWidth, viewportHeight, width, feather, glowRadius, rainbowDensity);
        }
    }

    private static void drawEdge(int shader, int startCorner, int endCorner,
                                  ClipPoint originalA, ClipPoint originalB,
                                  int viewportX, int viewportY, int viewportWidth, int viewportHeight,
                                  float width, float feather, float glowRadius, float rainbowDensity) {
        ClipPoint[] clipped = clipToFrustum(originalA, originalB);
        if (clipped == null) return;
        ClipPoint a = clipped[0];
        ClipPoint b = clipped[1];
        if (Math.abs(a.w) < 1.0E-8D || Math.abs(b.w) < 1.0E-8D) return;

        double ax = a.x / a.w;
        double ay = a.y / a.w;
        double bx = b.x / b.w;
        double by = b.y / b.w;
        double dxPixels = (bx - ax) * viewportWidth * 0.5D;
        double dyPixels = (by - ay) * viewportHeight * 0.5D;
        double length = Math.sqrt(dxPixels * dxPixels + dyPixels * dyPixels);
        if (length < 1.0E-4D) return;

        double directionX = dxPixels / length;
        double directionY = dyPixels / length;
        double perpendicularX = -directionY;
        double perpendicularY = directionX;
        double halfWidth = Math.max(0.5D, Math.min(64.0D, width)) * 0.5D;
        double extent = halfWidth + Math.max(0.0D, feather) + Math.max(0.0D, glowRadius) + 1.0D;
        double alongNdcX = directionX * extent * 2.0D / viewportWidth;
        double alongNdcY = directionY * extent * 2.0D / viewportHeight;
        double acrossNdcX = perpendicularX * extent * 2.0D / viewportWidth;
        double acrossNdcY = perpendicularY * extent * 2.0D / viewportHeight;

        GL20.glUniform1f(GL20.glGetUniformLocation(shader, "uLength"), (float) length);
        float hueStart = cornerHuePhase(startCorner, rainbowDensity);
        uniform1(shader, "uHueStart", hueStart);
        uniform1(shader, "uHueDelta", cornerHuePhase(endCorner, rainbowDensity) - hueStart);
        uniform2(shader, "uStart", (float) (viewportX + (ax + 1.0D) * viewportWidth * 0.5D),
                (float) (viewportY + (ay + 1.0D) * viewportHeight * 0.5D));
        uniform2(shader, "uDirection", (float) directionX, (float) directionY);
        GL11.glBegin(GL11.GL_QUADS);
        vertex(a, ax - alongNdcX + acrossNdcX, ay - alongNdcY + acrossNdcY);
        vertex(a, ax - alongNdcX - acrossNdcX, ay - alongNdcY - acrossNdcY);
        vertex(b, bx + alongNdcX - acrossNdcX, by + alongNdcY - acrossNdcY);
        vertex(b, bx + alongNdcX + acrossNdcX, by + alongNdcY + acrossNdcY);
        GL11.glEnd();
    }

    private static void vertex(ClipPoint source, double ndcX, double ndcY) {
        GL11.glVertex4d(ndcX * source.w, ndcY * source.w, source.z, source.w);
    }

    private static void drawNative(AxisAlignedBB box, BlockOutlineResolver.ResolvedOutline outline,
                                   float alphaMultiplier) {
        float previousWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        try {
            GL11.glLineWidth(clampNativeWidth(outline.lineWidth));
            float brightness = Math.min(1.0F, brightness());
            RenderGlobal.drawSelectionBoundingBox(box,
                    Math.min(1.0F, outline.red * brightness),
                    Math.min(1.0F, outline.green * brightness),
                    Math.min(1.0F, outline.blue * brightness),
                    outline.alpha * lineAlpha(alphaMultiplier));
        } finally {
            GL11.glLineWidth(previousWidth);
        }
    }

    private static float pulseAlpha() {
        if (!BlockOutlineConfig.pulseEnabled) return 1.0F;
        float period = MathUtil.clamp(BlockOutlineConfig.pulsePeriodMillis, 250.0F, 10000.0F);
        double phase = (System.currentTimeMillis() % (long) period) / period;
        float wave = 0.5F - 0.5F * (float) Math.cos(phase * Math.PI * 2.0D);
        return MathUtil.lerp(MathUtil.clamp(BlockOutlineConfig.pulseMinimumAlpha, 0.0F, 1.0F), 1.0F, wave);
    }

    private static float lineAlpha(float passMultiplier) {
        return MathUtil.clamp(passMultiplier, 0.0F, 1.0F)
                * MathUtil.clamp(BlockOutlineConfig.outlineOpacity, 0.0F, 1.0F);
    }

    private static float brightness() {
        return MathUtil.clamp(BlockOutlineConfig.outlineBrightness, 0.0F, 4.0F);
    }

    static float rainbowCyclePhase(long timeMillis, float cycleMillis) {
        double period = MathUtil.clamp(cycleMillis, 250.0F, 20000.0F);
        double cycles = timeMillis / period;
        return (float) (cycles - Math.floor(cycles));
    }

    static float cornerHuePhase(int corner, float density) {
        if (corner < 0 || corner >= LOCAL_CORNERS.length) return 0.0F;
        float[] point = LOCAL_CORNERS[corner];
        // Unequal axis weights sum to one cycle and give every shared cube corner a stable hue.
        float spatialPhase = point[0] * 0.31F + point[1] * 0.43F + point[2] * 0.26F;
        return spatialPhase * MathUtil.clamp(density, 0.25F, 4.0F);
    }

    private static float patternValue() {
        if (BlockOutlineConfig.PATTERN_DASHED.equals(BlockOutlineConfig.pattern)) return 1.0F;
        if (BlockOutlineConfig.PATTERN_DOTTED.equals(BlockOutlineConfig.pattern)) return 2.0F;
        return 0.0F;
    }

    private static float clampNativeWidth(float requested) {
        queryNativeWidthRange();
        float minimum = Float.isFinite(nativeMinimum) ? nativeMinimum : 1.0F;
        float maximum = Float.isFinite(nativeMaximum) ? nativeMaximum : minimum;
        return Math.max(minimum, Math.min(maximum, Math.max(0.01F, requested)));
    }

    private static void queryNativeWidthRange() {
        if (Float.isFinite(nativeMinimum) && Float.isFinite(nativeMaximum)) return;
        try {
            FloatBuffer range = BufferUtils.createFloatBuffer(2);
            GL11.glGetFloat(GL12.GL_ALIASED_LINE_WIDTH_RANGE, range);
            nativeMinimum = Math.max(0.01F, range.get(0));
            nativeMaximum = Math.max(nativeMinimum, range.get(1));
        } catch (Throwable ignored) {
            nativeMinimum = 1.0F;
            nativeMaximum = 1.0F;
        }
    }

    private static double[][] corners(AxisAlignedBB box) {
        return new double[][] {
                {box.minX, box.minY, box.minZ}, {box.maxX, box.minY, box.minZ},
                {box.minX, box.maxY, box.minZ}, {box.maxX, box.maxY, box.minZ},
                {box.minX, box.minY, box.maxZ}, {box.maxX, box.minY, box.maxZ},
                {box.minX, box.maxY, box.maxZ}, {box.maxX, box.maxY, box.maxZ}
        };
    }

    private static ClipPoint transform(FloatBuffer matrix, ClipPoint point) {
        return new ClipPoint(
                matrix.get(0) * point.x + matrix.get(4) * point.y + matrix.get(8) * point.z + matrix.get(12) * point.w,
                matrix.get(1) * point.x + matrix.get(5) * point.y + matrix.get(9) * point.z + matrix.get(13) * point.w,
                matrix.get(2) * point.x + matrix.get(6) * point.y + matrix.get(10) * point.z + matrix.get(14) * point.w,
                matrix.get(3) * point.x + matrix.get(7) * point.y + matrix.get(11) * point.z + matrix.get(15) * point.w);
    }

    private static ClipPoint[] clipToFrustum(ClipPoint a, ClipPoint b) {
        double start = 0.0D;
        double end = 1.0D;
        double[][] values = {
                {a.w + a.x, b.w + b.x}, {a.w - a.x, b.w - b.x},
                {a.w + a.y, b.w + b.y}, {a.w - a.y, b.w - b.y},
                {a.w + a.z, b.w + b.z}, {a.w - a.z, b.w - b.z}
        };
        for (double[] plane : values) {
            double first = plane[0];
            double second = plane[1];
            if (first < 0.0D && second < 0.0D) return null;
            if (first < 0.0D || second < 0.0D) {
                double crossing = first / (first - second);
                if (first < 0.0D) start = Math.max(start, crossing);
                else end = Math.min(end, crossing);
                if (start > end) return null;
            }
        }
        return new ClipPoint[] {lerp(a, b, start), lerp(a, b, end)};
    }

    private static ClipPoint lerp(ClipPoint a, ClipPoint b, double amount) {
        return new ClipPoint(a.x + (b.x - a.x) * amount, a.y + (b.y - a.y) * amount,
                a.z + (b.z - a.z) * amount, a.w + (b.w - a.w) * amount);
    }

    private static int getOrCreateProgram() {
        if (shaderUnavailable) return 0;
        if (program != 0) return program;
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER, read("block_outline.vsh"));
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, read("block_outline.fsh"));
            int created = GL20.glCreateProgram();
            GL20.glAttachShader(created, vertex);
            GL20.glAttachShader(created, fragment);
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(created, 8192));
            }
            GL20.glDetachShader(created, vertex);
            GL20.glDetachShader(created, fragment);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            program = created;
            return created;
        } catch (Throwable throwable) {
            shaderUnavailable = true;
            NfrUiEnhancements.LOGGER.warn("Screen-space block-outline shader is unavailable; using native lines", throwable);
            return 0;
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 8192);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    private static String read(String name) throws IOException {
        try (InputStream stream = BlockOutlineRenderer.class.getResourceAsStream(SHADER_ROOT + name)) {
            if (stream == null) throw new IOException("Missing shader " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void uniform1(int shader, String name, float value) {
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, name), value);
    }

    private static void uniform2(int shader, String name, float x, float y) {
        GL20.glUniform2f(GL20.glGetUniformLocation(shader, name), x, y);
    }

    private static final class ClipPoint {
        private final double x;
        private final double y;
        private final double z;
        private final double w;

        private ClipPoint(double x, double y, double z, double w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }
    }

    private static final class GlState {
        private final boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean textureEnabled = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        private final boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean alphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        private final FloatBuffer color = currentColor();

        private void restore() {
            GL20.glUseProgram(program);
            GL11.glDepthFunc(depthFunction);
            GL11.glDepthMask(depthMask);
            GL14.glBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            GL11.glShadeModel(shadeModel);
            GL11.glLineWidth(lineWidth);
            setEnabled(GL11.GL_DEPTH_TEST, depthEnabled);
            setEnabled(GL11.GL_BLEND, blendEnabled);
            setEnabled(GL11.GL_TEXTURE_2D, textureEnabled);
            setEnabled(GL11.GL_CULL_FACE, cullEnabled);
            setEnabled(GL11.GL_ALPHA_TEST, alphaTestEnabled);
            GL11.glColor4f(color.get(0), color.get(1), color.get(2), color.get(3));
        }

        private static FloatBuffer currentColor() {
            FloatBuffer value = BufferUtils.createFloatBuffer(4);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, value);
            return value;
        }

        private static void setEnabled(int capability, boolean enabled) {
            if (enabled) GL11.glEnable(capability);
            else GL11.glDisable(capability);
        }
    }
}
