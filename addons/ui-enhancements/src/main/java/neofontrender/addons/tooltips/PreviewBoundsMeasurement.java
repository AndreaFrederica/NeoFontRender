package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cached full-turn layout bounds. Entity and special-renderer bounds are conservative samples.
 *
 * <p>1.7.10 cannot enumerate a perspective-aware baked item model, so every silhouette is
 * measured by rendering the real preview path into an offscreen target and reading its alpha
 * channel back.</p>
 */
final class PreviewBoundsMeasurement {
    private static final int PROBE_PIXELS = 256;
    private static final int YAW_SAMPLES = 24;
    private static final PreviewRenderTarget PROBE = new PreviewRenderTarget();
    private static final PreviewRenderTarget TOOLTIP = new PreviewRenderTarget();
    private static final ByteBuffer ALPHA = BufferUtils.createByteBuffer(PROBE_PIXELS * PROBE_PIXELS);
    private static final Map<Key, Optional<PreviewModelBounds>> CACHE =
            new LinkedHashMap<Key, Optional<PreviewModelBounds>>(128, 0.75F, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<Key, Optional<PreviewModelBounds>> entry) {
                    return size() > 128;
                }
            };
    private static boolean measuring;

    private PreviewBoundsMeasurement() {}

    static PreviewModelBounds measure(NfrTooltipApi.PreviewRequest request) {
        if (measuring || !available()) return null;
        if (!(request instanceof NfrTooltipApi.ItemPreviewRequest)
                && !(request instanceof NfrTooltipApi.ArmorPreviewRequest)) return null;
        NfrTooltipApi.PreviewSize cell = nominalSize(request);
        if (stack(request) == null) return null;
        Key key = new Key(requestKey(request));
        return cached(key, () -> {
            boolean rotating = rotationSpeed(request) != 0;
            boolean swing = request instanceof NfrTooltipApi.ArmorPreviewRequest
                    && ((NfrTooltipApi.ArmorPreviewRequest) request).model() == NfrTooltipApi.ArmorPreviewModel.PLAYER
                    && "swing".equalsIgnoreCase(TooltipConfig.armorPlayerPose);
            PreviewModelBounds bounds = probe(cell, rotating, swing, (yaw, pose) ->
                    TooltipPreviewRenderers.renderRaw(request, 0, 0, cell, 1, yaw, pose));
            return bounds == null ? null : bounds.includeCell(cell.width(), cell.height());
        });
    }

    static PreviewModelBounds zoomItem(ItemStack stack, ItemZoomPresentation.Category category, int size) {
        if (measuring || !available() || stack == null) return null;
        PreviewStyleRegistry.Style style = PreviewStyleRegistry.INSTANCE.match(stack);
        boolean rotating = TooltipConfig.zoomOverlayRotation && "spin".equals(TooltipConfig.zoomOverlayMotion)
                && TooltipConfig.zoomOverlayRotationSpeed != 0;
        Key key = new Key("zoom|" + TooltipPreviewRenderers.animationKey(null, stack)
                + '|' + category + '|' + size + '|' + rotating + '|'
                + (style == null ? "default" : style.pitch(0) + ":" + style.roll(0)));
        return cached(key, () -> {
            PreviewModelBounds bounds = probe(new NfrTooltipApi.PreviewSize(size, size), rotating, false,
                    (yaw, unused) -> ItemZoomRenderer.renderModelRaw(stack, category, size, yaw));
            return bounds == null ? null : bounds.includeCell(size, size);
        });
    }

    private static PreviewModelBounds cached(Key key, Supplier<PreviewModelBounds> compute) {
        Optional<PreviewModelBounds> cached = CACHE.get(key);
        if (cached != null) return cached.orElse(null);
        PreviewModelBounds result = null;
        measuring = true;
        try {
            result = compute.get();
        } catch (RuntimeException | LinkageError failure) {
            if (neofontrender.addons.build.UiBuildFeatures.DIAGNOSTIC_LOGS) {
                TooltipModule.LOGGER.debug("Preview bounds measurement failed; using the configured cell", failure);
            }
        } finally {
            measuring = false;
        }
        CACHE.put(key, Optional.ofNullable(result));
        return result;
    }

    private interface ProbeDraw { void render(float yaw, float swing); }

    private static PreviewModelBounds probe(NfrTooltipApi.PreviewSize cell, boolean rotating,
                                            boolean swinging, ProbeDraw draw) {
        int extent = Math.max(128, Math.max(cell.width(), cell.height()) * 2);
        for (int attempt = 0; attempt < 4; attempt++, extent *= 2) {
            float scale = PROBE_PIXELS / (float) extent;
            float ox = (PROBE_PIXELS - cell.width() * scale) * 0.5F;
            float oy = (PROBE_PIXELS - cell.height() * scale) * 0.5F;
            if (!PROBE.render(PROBE_PIXELS, PROBE_PIXELS, 1, () -> {
                GL11.glTranslatef(ox, oy, 0);
                GL11.glScalef(scale, scale, scale);
                // Accumulate silhouettes in one color buffer, with fresh depth per pose.
                // Appearance, effects and sounds are deliberately absent from these probes.
                for (int pose = 0; pose < (swinging ? 5 : 1); pose++) {
                    for (int angle = 0; angle < (rotating ? YAW_SAMPLES : 1); angle++) {
                        GL11.glPushMatrix();
                        try (ModernTooltipRenderer.CallerGlState ignored =
                                     ModernTooltipRenderer.CallerGlState.capture()) {
                            TooltipPreviewRenderers.preparePreviewDepthLayer();
                            draw.render(angle * (360F / YAW_SAMPLES), pose / 4F);
                        } finally {
                            GL11.glPopMatrix();
                        }
                    }
                }
                readAlpha();
            })) return null;
            PreviewModelBounds pixels = PreviewModelBounds.alpha(ALPHA, PROBE_PIXELS, PROBE_PIXELS);
            if (pixels == null) return null;
            boolean clipped = pixels.left <= 1 || pixels.top <= 1
                    || pixels.right >= PROBE_PIXELS - 1 || pixels.bottom >= PROBE_PIXELS - 1;
            if (clipped && attempt < 3) continue;
            PreviewModelBounds bounds = new PreviewModelBounds((pixels.left - ox) / scale,
                    (pixels.top - oy) / scale, (pixels.right - ox) / scale, (pixels.bottom - oy) / scale);
            // Angular/pose samples are approximate. Include raster error and an 8% guard;
            // the final preview surface also contains any exceptional animated overshoot.
            return bounds.pad(2 / scale + Math.max(bounds.width(), bounds.height()) * 0.08F);
        }
        return null;
    }

    private static void readAlpha() {
        int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_PIXEL_STORE_BIT);
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            ALPHA.clear();
            GL11.glReadPixels(0, 0, PROBE_PIXELS, PROBE_PIXELS, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, ALPHA);
        } finally {
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
        }
    }

    static boolean renderTooltip(NfrTooltipApi.PreviewRequest request, int x, int y,
                                 NfrTooltipApi.PreviewSize size, float animation, float spin, float swing) {
        if (measuring || !available()) return false;
        PreviewModelBounds measured = measure(request);
        NfrTooltipApi.PreviewSize original = nominalSize(request);
        PreviewModelBounds bounds = measured == null
                ? new PreviewModelBounds(0, 0, original.width(), original.height()) : measured;
        try (ModernTooltipRenderer.CallerGlState ignored =
                     ModernTooltipRenderer.CallerGlState.capture()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            int pixelScale = new ScaledResolution(minecraft, minecraft.displayWidth,
                    minecraft.displayHeight).getScaleFactor();
            if (!TOOLTIP.render(size.width(), size.height(), pixelScale, () -> {
                applyFit(bounds, size.width(), size.height(), 0);
                TooltipPreviewRenderers.renderRaw(request, 0, 0, original, animation, spin, swing);
            })) return false;
            TOOLTIP.composite(x, y, size.width(), size.height(), 1);
        }
        return true;
    }

    static void applyFit(PreviewModelBounds bounds, int width, int height, int inset) {
        PreviewModelBounds.Placement placement = bounds.fit(Math.max(1, width - 2 * inset), Math.max(1, height - 2 * inset));
        GL11.glTranslatef(inset + placement.x, inset + placement.y, 0);
        GL11.glScalef(placement.scale, placement.scale, placement.scale);
    }

    static NfrTooltipApi.PreviewSize nominalSize(NfrTooltipApi.PreviewRequest request) {
        if (request instanceof NfrTooltipApi.ItemPreviewRequest) {
            NfrTooltipApi.ItemPreviewRequest item = (NfrTooltipApi.ItemPreviewRequest) request;
            return new NfrTooltipApi.PreviewSize(Math.max(1, item.width()), Math.max(1, item.height()));
        }
        NfrTooltipApi.ArmorPreviewRequest armor = (NfrTooltipApi.ArmorPreviewRequest) request;
        return new NfrTooltipApi.PreviewSize(Math.max(1, armor.width()), Math.max(1, armor.height()));
    }

    static float rotationSpeed(NfrTooltipApi.PreviewRequest request) {
        return request instanceof NfrTooltipApi.ItemPreviewRequest
                ? ((NfrTooltipApi.ItemPreviewRequest) request).rotationSpeed()
                : ((NfrTooltipApi.ArmorPreviewRequest) request).rotationSpeed();
    }

    private static ItemStack stack(NfrTooltipApi.PreviewRequest request) {
        return request instanceof NfrTooltipApi.ItemPreviewRequest
                ? ((NfrTooltipApi.ItemPreviewRequest) request).stack()
                : ((NfrTooltipApi.ArmorPreviewRequest) request).stack();
    }

    private static String requestKey(NfrTooltipApi.PreviewRequest request) {
        NfrTooltipApi.PreviewSize cell = nominalSize(request);
        String key = TooltipPreviewRenderers.animationKey(request, stack(request))
                + '|' + cell.width() + '|' + cell.height() + '|' + (rotationSpeed(request) != 0);
        if (request instanceof NfrTooltipApi.ItemPreviewRequest) {
            NfrTooltipApi.ItemPreviewRequest item = (NfrTooltipApi.ItemPreviewRequest) request;
            return key + '|' + item.scale() + '|' + item.pitch() + '|' + item.roll();
        }
        NfrTooltipApi.ArmorPreviewRequest armor = (NfrTooltipApi.ArmorPreviewRequest) request;
        Minecraft mc = Minecraft.getMinecraft();
        key += '|' + Float.toString(armor.scale()) + '|' + armor.pitch() + '|'
                + TooltipConfig.armorStandBasePlate + '|' + TooltipConfig.armorPlayerSneaking + '|'
                + TooltipConfig.armorPlayerPose + '|' + TooltipConfig.armorPlayerCopyHands;
        if (mc.thePlayer != null) {
            ResourceLocation skin = mc.thePlayer.getLocationSkin();
            key += '|' + mc.thePlayer.getGameProfile().getId().toString() + '|' + skin;
            if (TooltipConfig.armorPlayerCopyHands) {
                // 1.7.10 has a single hand slot, so only the held item can be copied.
                key += TooltipPreviewRenderers.animationKey(null, mc.thePlayer.getHeldItem());
            }
        }
        return key;
    }

    private static boolean available() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.theWorld != null && RenderItem.getInstance() != null
                && OpenGlHelper.isFramebufferEnabled();
    }

    static void clear() {
        CACHE.clear();
        releaseTargets();
    }

    static void releaseTargets() {
        PROBE.release();
        TOOLTIP.release();
    }

    private static final class Key {
        final String request;
        Key(String request) { this.request = request; }
        @Override public int hashCode() { return request.hashCode(); }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            return request.equals(((Key) other).request);
        }
    }
}
