package neofontrender.core.font.postprocess;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.NeoFontRender;
import neofontrender.api.text.gl.TextGlComponent;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.api.text.effect.TextEffectDefinition;
import neofontrender.api.text.effect.TextEffectRegistry;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

/** Lower-level draw hook reserved for pooled FBO/shader passes. */
final class BrilliantCaptureRenderer implements TextGlComponent {
    static final BrilliantCaptureRenderer INSTANCE = new BrilliantCaptureRenderer();
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static Target target;
    private static final Map<String, Integer> PROGRAMS = new HashMap<>();
    private static int activeProgram;
    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static long lastParticleTick = Long.MIN_VALUE;
    private static volatile String lastFailure = "";
    private static long captureCount;
    private static long compositeCount;

    private BrilliantCaptureRenderer() {}

    /** Updates the particle simulation independently of individual text draw calls. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) updateParticles();
    }

    /** Flushes particles after GUI/HUD content so they are composited as one overlay. */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            renderParticles(Minecraft.getMinecraft());
        }
    }

    /**
     * A book, chat screen, and other GuiScreen-backed interfaces are rendered outside the HUD
     * overlay event. Draw after the screen itself so Brilliant particles remain above its text.
     */
    @SubscribeEvent
    public void onGuiScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        renderParticles(Minecraft.getMinecraft());
    }

    @Override public String id() { return "brilliant_text:gl"; }
    @Override public int priority() { return 100; }
    @Override public boolean isAvailable() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc != null && mc.displayWidth > 0 && mc.displayHeight > 0;
        } catch (Exception | LinkageError error) {
            return false;
        }
    }

    @Override public String status() {
        if (!lastFailure.isEmpty()) return "fallback:" + lastFailure;
        if (!PROGRAMS.isEmpty() && target != null) return "ready:capture=" + captureCount
                + ",composite=" + compositeCount;
        if (target != null) return "fbo-ready/shader-pending";
        return isAvailable() ? "ready-on-first-draw" : "unavailable";
    }

    @Override
    public TextRenderResult draw(TextRenderResult source, float x, float y, float alpha,
                                 java.util.List<StructuredEffectSpan> effects) {
        return new CapturedResult(source, this, effects);
    }

    private void drawCaptured(TextRenderResult delegate, float x, float y, float alpha,
                              java.util.List<StructuredEffectSpan> effects) {
        if (delegate == null || delegate == TextRenderResult.EMPTY || Boolean.TRUE.equals(ACTIVE.get())) {
            if (delegate != null) delegate.draw(x, y, alpha);
            return;
        }
        ACTIVE.set(true);
        try {
            if (!renderCaptured(delegate, x, y, alpha, effects)) delegate.draw(x, y, alpha);
        } finally {
            ACTIVE.remove();
        }
    }

    private static final class CapturedResult implements TextRenderResult {
        private final TextRenderResult delegate;
        private final BrilliantCaptureRenderer owner;
        private final java.util.List<StructuredEffectSpan> effects;
        private CapturedResult(TextRenderResult delegate, BrilliantCaptureRenderer owner,
                               java.util.List<StructuredEffectSpan> effects) {
            this.delegate = delegate; this.owner = owner; this.effects = effects;
        }
        @Override public float advance() { return delegate.advance(); }
        @Override public float visualLeft() { return delegate.visualLeft(); }
        @Override public float visualRight() { return delegate.visualRight(); }
        @Override public float visualTop() { return delegate.visualTop(); }
        @Override public float visualBottom() { return delegate.visualBottom(); }
        @Override public void draw(float x, float y, float alpha) {
            owner.drawCaptured(delegate, x, y, alpha, effects);
        }
    }

    private static boolean renderCaptured(TextRenderResult delegate, float x, float y, float alpha,
                                          java.util.List<StructuredEffectSpan> effects) {
        try (CallerGlState callerState = CallerGlState.capture();
             CallerMatrices ignored = CallerMatrices.capture()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) return false;
            ensureTarget(mc.displayWidth, mc.displayHeight);
            if (target == null) return false;
            target.bind();
            boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            if (scissorEnabled) GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GlStateManager.colorMask(true, true, true, true);
            GL11.glColorMask(true, true, true, true);
            GlStateManager.clearColor(0, 0, 0, 0);
            GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
            if (scissorEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            delegate.draw(x, y, alpha);
            captureCount++;

            callerState.restoreFramebufferAndViewport();
            drawPass(mc, x, y, delegate, effects);
            compositeCount++;
            lastFailure = "";
            return true;
        } catch (RuntimeException | LinkageError error) {
            lastFailure = error.getClass().getSimpleName();
            NeoFontRender.LOGGER.warn("Brilliant Text GL capture failed; using direct text draw", error);
            return false;
        }
    }

    private static void drawPass(Minecraft mc, float x, float y, TextRenderResult result,
                                 java.util.List<StructuredEffectSpan> effects) {
        ScaledResolution res = new ScaledResolution(mc);
        float scale = res.getScaleFactor();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GL11.glOrtho(0, mc.displayWidth, mc.displayHeight, 0, -1, 1);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.disableAlpha();
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GlStateManager.disableDepth();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GlStateManager.depthMask(false);
            GL11.glDepthMask(false);
            GlStateManager.disableLighting();
            GL11.glDisable(GL11.GL_LIGHTING);
            GlStateManager.disableFog();
            GL11.glDisable(GL11.GL_FOG);
            GlStateManager.disableCull();
            GL11.glDisable(GL11.GL_CULL_FACE);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GL11.glEnable(GL11.GL_BLEND);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
            GlStateManager.color(1, 1, 1, 1);
            selectTextureUnit(GL13.GL_TEXTURE0);
            GlStateManager.bindTexture(target.texture);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, target.texture);
            List<EffectRegion> regions = effectRegions(effects, x, y, result, scale);
            setCompositeBlend(true);
            drawPassthroughRegions(mc, x, y, result, scale, regions);
            if (!regions.isEmpty()) {
                setCompositeBlend(false);
                for (EffectRegion region : regions) {
                    StructuredEffectSpan effect = region.effect;
                    TextEffectDefinition definition = TextEffectRegistry.getOrDefault(effect.effectId());
                    int effectProgram = ensureProgram(definition);
                    if (effectProgram == 0) continue;
                    activeProgram = effectProgram;
                    ARBShaderObjects.glUseProgramObjectARB(effectProgram);
                    int sampler = ARBShaderObjects.glGetUniformLocationARB(effectProgram, "u_texture");
                    if (sampler >= 0) ARBShaderObjects.glUniform1iARB(sampler, 0);
                    setUniform2f("u_textureSize", mc.displayWidth, mc.displayHeight);
                    setUniform2f("u_sampleSize", mc.displayWidth, mc.displayHeight);
                    setUniform4f("u_effectBounds", region.left, region.top,
                            region.right, region.bottom);
                    setUniform4f("u_textColor", color(effect, "textColor", 0xFFFFFFFF));
                    setUniform4f("u_outlineColor", color(effect, "outlineColor", 0));
                    setUniform4f("u_glowColor", color(effect, "glowColor", 0));
                    setUniform1f("u_effectType", effectType(effect));
                    setUniform1f("u_time", (System.currentTimeMillis() % 1000000L) / 1000.0F);
                    drawRegion(mc, region.left, region.top, region.right, region.bottom);
                    spawnParticle(mc, effect, x, y, result);
                }
            }
            ARBShaderObjects.glUseProgramObjectARB(0);
        } finally {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
    }

    private static List<EffectRegion> effectRegions(List<StructuredEffectSpan> effects, float x, float y,
                                                    TextRenderResult result, float scale) {
        List<EffectRegion> regions = new ArrayList<>();
        if (effects == null) return regions;
        for (StructuredEffectSpan effect : effects) {
            float left = (x + parameter(effect, "left", result.visualLeft())) * scale - 1.0F;
            float top = (y + parameter(effect, "top", result.visualTop())) * scale - 1.0F;
            float right = (x + parameter(effect, "right", result.visualRight())) * scale + 1.0F;
            float bottom = (y + parameter(effect, "bottom", result.visualBottom())) * scale + 1.0F;
            if (right > left && bottom > top) regions.add(new EffectRegion(effect, left, top, right, bottom));
        }
        regions.sort(Comparator.comparingDouble(region -> region.left));
        return regions;
    }

    private static void drawPassthroughRegions(Minecraft mc, float x, float y,
                                               TextRenderResult result, float scale,
                                               List<EffectRegion> effects) {
        float left = (x + result.visualLeft()) * scale;
        float top = (y + result.visualTop()) * scale;
        float right = (x + result.visualRight()) * scale;
        float bottom = (y + result.visualBottom()) * scale;
        float cursor = left;
        ARBShaderObjects.glUseProgramObjectARB(0);
        for (EffectRegion effect : effects) {
            float effectLeft = Math.max(left, effect.left + 1.0F);
            if (effectLeft > cursor) drawRegion(mc, cursor, top, effectLeft, bottom);
            cursor = Math.max(cursor, Math.min(right, effect.right - 1.0F));
        }
        if (cursor < right) drawRegion(mc, cursor, top, right, bottom);
    }

    private static void drawRegion(Minecraft mc, float left, float top, float right, float bottom) {
        if (right <= left || bottom <= top) return;
        float u0 = left / mc.displayWidth;
        float u1 = right / mc.displayWidth;
        float v0 = 1.0F - top / mc.displayHeight;
        float v1 = 1.0F - bottom / mc.displayHeight;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(left, top, 0).tex(u0, v0).endVertex();
        buffer.pos(left, bottom, 0).tex(u0, v1).endVertex();
        buffer.pos(right, bottom, 0).tex(u1, v1).endVertex();
        buffer.pos(right, top, 0).tex(u1, v0).endVertex();
        Tessellator.getInstance().draw();
    }

    private static void setCompositeBlend(boolean premultiplied) {
        int source = premultiplied ? GL11.GL_ONE : GL11.GL_SRC_ALPHA;
        GlStateManager.tryBlendFuncSeparate(source, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL14.glBlendFuncSeparate(source, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static final class EffectRegion {
        final StructuredEffectSpan effect;
        final float left, top, right, bottom;

        EffectRegion(StructuredEffectSpan effect, float left, float top, float right, float bottom) {
            this.effect = effect;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private static float parameter(StructuredEffectSpan effect, String key, float fallback) {
        try { return Float.parseFloat(effect.parameters().get(key)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static float effectType(StructuredEffectSpan effect) {
        TextEffectDefinition definition = TextEffectRegistry.get(effect.effectId());
        return definition == null ? 0.0F : definition.shaderType();
    }

    private static float[] color(StructuredEffectSpan effect, String key, int fallback) {
        int value = fallback;
        try { value = (int) Long.parseLong(effect.parameters().get(key), 16); }
        catch (RuntimeException ignored) { }
        return new float[]{((value >>> 16) & 255) / 255.0F, ((value >>> 8) & 255) / 255.0F,
                (value & 255) / 255.0F, ((value >>> 24) & 255) / 255.0F};
    }

    private static void spawnParticle(Minecraft mc, StructuredEffectSpan effect, float x, float y,
                                      TextRenderResult result) {
        if (effectType(effect) > 2.5F) {
            spawnFlameParticle(effect, x, y, result);
            return;
        }
        String texture = effect.parameters().get("particleTexture");
        int rarity = integer(effect, "particleRarity", 100);
        int lifetime = integer(effect, "particleLifetime", 200);
        if (texture == null || texture.isEmpty() || rarity <= 0 || lifetime <= 0
                || (int) (Math.random() * rarity) != 0) return;
        float left = parameter(effect, "left", result.visualLeft());
        float right = parameter(effect, "right", result.visualRight());
        float top = parameter(effect, "top", result.visualTop());
        float bottom = parameter(effect, "bottom", result.visualBottom());
        int[] dimensions = range(effect.parameters().get("particleDimensions"), 2, 2);
        int[] rotation = range(effect.parameters().get("particleRotation"), 0, 0);
        float[] spin = floatRange(effect.parameters().get("particleRotationPerFrame"), 0, 0);
        int size = dimensions[0] + (int) (Math.random() * Math.max(1, dimensions[1] - dimensions[0] + 1));
        float angle = rotation[0] + (float) Math.random() * Math.max(0, rotation[1] - rotation[0]);
        float speed = spin[0] + (float) Math.random() * Math.max(0, spin[1] - spin[0]);
        PARTICLES.add(new Particle(x + left + (float) Math.random() * Math.max(1, right - left),
                y + top + (float) Math.random() * Math.max(1, bottom - top),
                lifetime, size, angle, speed, parseHex(effect.parameters().get("particleColor"), 0xFFFFFFFF),
                new ResourceLocation(texture)));
    }

    /** Matches the original Brilliant Text flame particle rather than the configurable sprite particle. */
    private static void spawnFlameParticle(StructuredEffectSpan effect, float x, float y,
                                           TextRenderResult result) {
        Random random = new Random();
        float left = parameter(effect, "left", result.visualLeft());
        float right = parameter(effect, "right", result.visualRight());
        float top = parameter(effect, "top", result.visualTop());
        float bottom = parameter(effect, "bottom", result.visualBottom());
        int lifetime = 100 + random.nextInt(100);
        int color = random.nextBoolean() ? 0xFFFF4433 : 0xFFCC5500;
        int reducedLifetime = random.nextInt(100);
        PARTICLES.add(Particle.flame(
                x + left + (float) random.nextDouble() * Math.max(1, right - left),
                y + top + (float) random.nextDouble() * Math.max(1, bottom - top),
                lifetime, color, reducedLifetime,
                (random.nextFloat() - 0.5F) * 0.1F));
    }

    private static void renderParticles(Minecraft mc) {
        if (mc == null || PARTICLES.isEmpty()) return;
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        ScaledResolution res = new ScaledResolution(mc);
        if (res.getScaledWidth() <= 0 || res.getScaledHeight() <= 0) return;
        try (CallerGlState ignoredState = CallerGlState.capture();
             CallerMatrices ignoredMatrices = CallerMatrices.capture()) {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GL11.glOrtho(0, res.getScaledWidth(), res.getScaledHeight(), 0, -1, 1);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            selectTextureUnit(GL13.GL_TEXTURE0);
            GlStateManager.enableTexture2D();
            GlStateManager.disableDepth();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1, 1, 1, 1);
            for (Particle particle : PARTICLES) {
                try { mc.getTextureManager().bindTexture(particle.texture); }
                catch (RuntimeException ignored) { continue; }
                float half = particle.size;
                float alpha = particle.life / (float) particle.maxLife;
                float rad = (float) Math.toRadians(particle.rotation);
                float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
                float cx = particle.x, cy = particle.y;
                float[] px = {-half, -half, half, half}, py = {-half, half, half, -half};
                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (int n = 0; n < 4; n++) {
                    float rx = cx + px[n] * cos - py[n] * sin;
                    float ry = cy + px[n] * sin + py[n] * cos;
                    int argb = particle.color;
                    buffer.pos(rx, ry, 0).tex(n == 0 || n == 1 ? 0 : 1, n == 0 || n == 3 ? 0 : 1)
                            .color((argb >> 16 & 255) / 255.0F, (argb >> 8 & 255) / 255.0F,
                                    (argb & 255) / 255.0F, ((argb >>> 24) & 255) / 255.0F * alpha).endVertex();
                }
                Tessellator.getInstance().draw();
            }
        }
    }

    private static void updateParticles() {
        long tick = System.currentTimeMillis() / 50L;
        if (tick == lastParticleTick || PARTICLES.isEmpty()) return;
        lastParticleTick = tick;
        for (int i = PARTICLES.size() - 1; i >= 0; i--) {
            Particle particle = PARTICLES.get(i);
            particle.life--;
            if (particle.flame) {
                float delta = Math.max(0.0F, Math.min(1.0F,
                        (particle.life - particle.reducedLifetime) / (float) particle.maxLife));
                int red = (int) (((particle.originalColor >>> 16) & 255) * delta);
                int green = (int) (((particle.originalColor >>> 8) & 255) * delta);
                int blue = (int) ((particle.originalColor & 255) * delta);
                particle.color = 0xFF000000 | (red << 16) | (green << 8) | blue;
                particle.x += particle.velocityX;
                particle.y -= 0.1F;
            } else {
                particle.y -= 0.1F;
            }
            particle.rotation += particle.spin;
            if (particle.life <= 0) PARTICLES.remove(i);
        }
    }

    private static int integer(StructuredEffectSpan effect, String key, int fallback) {
        try { return Integer.parseInt(effect.parameters().get(key)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int parseHex(String value, int fallback) {
        try { return (int) Long.parseLong(value, 16); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int[] range(String value, int low, int high) {
        try { String[] p = value.split("-"); return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])}; }
        catch (RuntimeException ignored) { return new int[]{low, high}; }
    }

    private static float[] floatRange(String value, float low, float high) {
        try { String[] p = value.split("-"); return new float[]{Float.parseFloat(p[0]), Float.parseFloat(p[1])}; }
        catch (RuntimeException ignored) { return new float[]{low, high}; }
    }

    private static final class Particle {
        float x, y, rotation, spin, velocityX;
        int life, maxLife, size, color, originalColor, reducedLifetime;
        boolean flame;
        ResourceLocation texture;
        Particle(float x, float y, int life, int size, float rotation, float spin, int color, ResourceLocation texture) {
            this.x = x; this.y = y; this.life = life; this.maxLife = life; this.size = size;
            this.rotation = rotation; this.spin = spin; this.color = color; this.texture = texture;
        }

        static Particle flame(float x, float y, int life, int color, int reducedLifetime, float velocityX) {
            Particle particle = new Particle(x, y, life, 1 + (int) (Math.random() * 2), 0, 1.0F,
                    color, new ResourceLocation("neofontrender", "textures/particles/glow_2.png"));
            particle.flame = true;
            particle.originalColor = color;
            particle.reducedLifetime = reducedLifetime;
            particle.velocityX = velocityX;
            return particle;
        }
    }

    private static void ensureTarget(int width, int height) {
        if (target == null) target = new Target(width, height);
        else if (target.width != width || target.height != height) target.resize(width, height);
    }

    private static int ensureProgram(TextEffectDefinition definition) {
        String key = definition.id() + "|" + definition.vertexShader() + "|" + definition.fragmentShader();
        Integer cached = PROGRAMS.get(key);
        if (cached != null) return cached;
        try {
            int vertexShader = compile(GL20.GL_VERTEX_SHADER, definition.vertexShader());
            int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, definition.fragmentShader());
            if (vertexShader == 0 || fragmentShader == 0) return 0;
            int effectProgram = ARBShaderObjects.glCreateProgramObjectARB();
            ARBShaderObjects.glAttachObjectARB(effectProgram, vertexShader);
            ARBShaderObjects.glAttachObjectARB(effectProgram, fragmentShader);
            ARBShaderObjects.glLinkProgramARB(effectProgram);
            if (ARBShaderObjects.glGetObjectParameteriARB(effectProgram,
                    ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == 0) {
                String log = GL20.glGetProgramInfoLog(effectProgram, 8192);
                NeoFontRender.LOGGER.error("Brilliant Text shader link failed: {}", log);
                lastFailure = "shader-link";
                ARBShaderObjects.glDeleteObjectARB(effectProgram);
                return 0;
            }
            PROGRAMS.put(key, effectProgram);
            return effectProgram;
        } catch (Exception | LinkageError error) {
            lastFailure = "shader-init";
            NeoFontRender.LOGGER.error("Brilliant Text shader initialization failed", error);
            return 0;
        }
    }

    private static int compile(int type, String path) throws Exception {
        ResourceLocation location = path.indexOf(':') >= 0
                ? new ResourceLocation(path)
                : new ResourceLocation("neofontrender", path);
        InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
        byte[] data;
        try (InputStream input = stream) { data = input.readAllBytes(); }
        int shader = ARBShaderObjects.glCreateShaderObjectARB(type);
        ARBShaderObjects.glShaderSourceARB(shader, new String(data, StandardCharsets.UTF_8));
        ARBShaderObjects.glCompileShaderARB(shader);
        if (ARBShaderObjects.glGetObjectParameteriARB(shader,
                ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) != 0) return shader;
        String log = GL20.glGetShaderInfoLog(shader, 8192);
        NeoFontRender.LOGGER.error("Brilliant Text shader '{}' failed to compile: {}", path, log);
        lastFailure = "shader-compile";
        ARBShaderObjects.glDeleteObjectARB(shader);
        return 0;
    }

    private static void setUniform1f(String name, float value) {
        int location = ARBShaderObjects.glGetUniformLocationARB(activeProgram, name);
        if (location >= 0) ARBShaderObjects.glUniform1fARB(location, value);
    }

    private static void setUniform2f(String name, float a, float b) {
        int location = ARBShaderObjects.glGetUniformLocationARB(activeProgram, name);
        if (location >= 0) ARBShaderObjects.glUniform2fARB(location, a, b);
    }

    private static void setUniform4f(String name, float a, float b, float c, float d) {
        int location = ARBShaderObjects.glGetUniformLocationARB(activeProgram, name);
        if (location >= 0) ARBShaderObjects.glUniform4fARB(location, a, b, c, d);
    }

    private static void setUniform4f(String name, float[] value) {
        setUniform4f(name, value[0], value[1], value[2], value[3]);
    }

    private static int[] readViewport() {
        IntBuffer values = BufferUtils.createIntBuffer(4);
        GL11.glGetInteger(GL11.GL_VIEWPORT, values);
        return new int[]{values.get(0), values.get(1), values.get(2), values.get(3)};
    }

    private static void selectTextureUnit(int unit) {
        GlStateManager.setActiveTexture(unit);
        GL13.glActiveTexture(unit);
    }

    /** Restores both the OpenGL driver and Minecraft's cached view of mutable render state. */
    private static final class CallerGlState implements AutoCloseable {
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int[] viewport = readViewport();
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final TextureUnitState[] textureUnits = readTextureUnits(activeTexture);
        private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        private final boolean light0 = GL11.glIsEnabled(GL11.GL_LIGHT0);
        private final boolean light1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
        private final boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        private final int blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        private final int alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        private final float alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
        private final boolean rescaleNormal = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
        private final int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        private final boolean[] colorMask = readColorMask();
        private final float[] color = readFloat4(GL11.GL_CURRENT_COLOR);
        private final float[] clearColor = readFloat4(GL11.GL_COLOR_CLEAR_VALUE);
        private boolean closed;

        private CallerGlState() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        }

        static CallerGlState capture() { return new CallerGlState(); }

        void restoreFramebufferAndViewport() {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            GL11.glPopAttrib();

            restoreFramebufferAndViewport();
            GL20.glUseProgram(program);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            restoreToggle(lighting, GL11.GL_LIGHTING,
                    GlStateManager::enableLighting, GlStateManager::disableLighting);
            restoreToggle(light0, GL11.GL_LIGHT0,
                    () -> GlStateManager.enableLight(0), () -> GlStateManager.disableLight(0));
            restoreToggle(light1, GL11.GL_LIGHT1,
                    () -> GlStateManager.enableLight(1), () -> GlStateManager.disableLight(1));
            restoreToggle(colorMaterial, GL11.GL_COLOR_MATERIAL,
                    GlStateManager::enableColorMaterial, GlStateManager::disableColorMaterial);
            restoreToggle(depth, GL11.GL_DEPTH_TEST,
                    GlStateManager::enableDepth, GlStateManager::disableDepth);
            GlStateManager.depthMask(depthMask);
            GL11.glDepthMask(depthMask);
            GlStateManager.depthFunc(depthFunction);
            GL11.glDepthFunc(depthFunction);
            restoreToggle(blend, GL11.GL_BLEND,
                    GlStateManager::enableBlend, GlStateManager::disableBlend);
            GlStateManager.alphaFunc(alphaFunction, alphaReference);
            GL11.glAlphaFunc(alphaFunction, alphaReference);
            restoreToggle(alpha, GL11.GL_ALPHA_TEST,
                    GlStateManager::enableAlpha, GlStateManager::disableAlpha);
            restoreToggle(cull, GL11.GL_CULL_FACE,
                    GlStateManager::enableCull, GlStateManager::disableCull);
            restoreToggle(fog, GL11.GL_FOG,
                    GlStateManager::enableFog, GlStateManager::disableFog);
            restoreToggle(rescaleNormal, GL12.GL_RESCALE_NORMAL,
                    GlStateManager::enableRescaleNormal, GlStateManager::disableRescaleNormal);
            GlStateManager.shadeModel(shadeModel);
            GL11.glShadeModel(shadeModel);
            GlStateManager.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GlStateManager.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            GL11.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            GlStateManager.color(color[0], color[1], color[2], color[3]);
            GL11.glColor4f(color[0], color[1], color[2], color[3]);
            restoreTextureUnits(textureUnits, activeTexture);
        }

        private static void restoreToggle(boolean enabled, int capability,
                                          Runnable enable, Runnable disable) {
            if (enabled) enable.run();
            else disable.run();
            if (enabled) GL11.glEnable(capability);
            else GL11.glDisable(capability);
        }

        private static boolean[] readColorMask() {
            IntBuffer values = BufferUtils.createIntBuffer(4);
            GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, values);
            return new boolean[]{values.get(0) != 0, values.get(1) != 0,
                    values.get(2) != 0, values.get(3) != 0};
        }

        private static float[] readFloat4(int property) {
            FloatBuffer values = BufferUtils.createFloatBuffer(4);
            GL11.glGetFloat(property, values);
            return new float[]{values.get(0), values.get(1), values.get(2), values.get(3)};
        }

        private static TextureUnitState[] readTextureUnits(int originalActiveTexture) {
            boolean originalIsItemUnit = originalActiveTexture >= GL13.GL_TEXTURE0
                    && originalActiveTexture <= GL13.GL_TEXTURE2;
            TextureUnitState[] states = new TextureUnitState[originalIsItemUnit ? 3 : 4];
            for (int i = 0; i < 3; i++) {
                int unit = GL13.GL_TEXTURE0 + i;
                GL13.glActiveTexture(unit);
                states[i] = new TextureUnitState(unit,
                        GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            }
            if (!originalIsItemUnit) {
                GL13.glActiveTexture(originalActiveTexture);
                states[3] = new TextureUnitState(originalActiveTexture,
                        GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            }
            GL13.glActiveTexture(originalActiveTexture);
            return states;
        }

        private static void restoreTextureUnits(TextureUnitState[] states,
                                                int originalActiveTexture) {
            for (TextureUnitState state : states) {
                selectTextureUnit(state.unit);
                restoreToggle(state.enabled, GL11.GL_TEXTURE_2D,
                        GlStateManager::enableTexture2D, GlStateManager::disableTexture2D);
                GlStateManager.bindTexture(state.binding);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.binding);
            }
            selectTextureUnit(originalActiveTexture);
        }
    }

    private static final class TextureUnitState {
        private final int unit;
        private final boolean enabled;
        private final int binding;

        private TextureUnitState(int unit, boolean enabled, int binding) {
            this.unit = unit;
            this.enabled = enabled;
            this.binding = binding;
        }
    }

    /** Protects the caller's projection/model-view stacks and original matrix mode. */
    private static final class CallerMatrices implements AutoCloseable {
        private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        private boolean closed;

        private CallerMatrices() {
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(matrixMode);
        }

        static CallerMatrices capture() { return new CallerMatrices(); }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(matrixMode);
        }
    }

    private static final class Target {
        private final int framebuffer;
        private final int texture;
        private int width;
        private int height;
        private Target(int width, int height) {
            this.framebuffer = GL30.glGenFramebuffers();
            this.texture = GL11.glGenTextures();
            resize(width, height);
        }
        private void resize(int width, int height) {
            this.width = width; this.height = height;
            int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            try {
                selectTextureUnit(GL13.GL_TEXTURE0);
                GlStateManager.bindTexture(texture);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                        GL11.GL_TEXTURE_2D, texture, 0);
                if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)
                        != GL30.GL_FRAMEBUFFER_COMPLETE) {
                    throw new IllegalStateException("Brilliant text framebuffer is incomplete");
                }
            } finally {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            }
        }
        private void bind() {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GlStateManager.viewport(0, 0, width, height);
        }
    }
}
