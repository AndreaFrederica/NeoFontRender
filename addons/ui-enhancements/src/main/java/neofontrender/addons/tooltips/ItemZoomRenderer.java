package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Draws into ItemZoomRenderTarget in GUI pixels, independent of tooltip layout and animation. */
final class ItemZoomRenderer {
    private static final Map<IBakedModel, Float> RADII = new IdentityHashMap<>();

    private ItemZoomRenderer() {}

    static void render(ItemStack stack, ItemZoomPresentation.Category category, String mode, int size) {
        boolean sway = TooltipConfig.zoomOverlayRotation && "sway".equals(TooltipConfig.zoomOverlayMotion);
        GlStateManager.pushMatrix();
        try {
            if (sway) {
                GlStateManager.translate(size * 0.5F, size * 0.5F, 0);
                GlStateManager.rotate((float) Math.sin(System.nanoTime() / 800_000_000.0D) * 2,
                        0, 0, 1);
                GlStateManager.translate(-size * 0.5F, -size * 0.5F, 0);
            }
            if ("2d".equals(mode)) {
                renderIcon(stack, size);
            } else if (category == ItemZoomPresentation.Category.EQUIPMENT) {
                TooltipPreviewRenderers.renderZoomArmor(armorRequest(stack, size), size);
            } else {
                renderModel(stack, category, size);
            }
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }

    static void renderIcon(ItemStack stack, int size) {
        RenderItem renderer = Minecraft.getMinecraft().getRenderItem();
        float oldZ = renderer.zLevel;
        GlStateManager.pushMatrix();
        try {
            RenderHelper.enableGUIStandardItemLighting();
            // Margin also accommodates the optional 2-degree sway.
            GlStateManager.translate(size * 0.06F, size * 0.06F, 0);
            GlStateManager.scale(size * 0.88F / 16, size * 0.88F / 16, 1);
            renderer.zLevel = 0;
            renderer.renderItemAndEffectIntoGUI(Minecraft.getMinecraft().player, stack, 0, 0);
        } finally {
            renderer.zLevel = oldZ;
            GlStateManager.popMatrix();
        }
    }

    static NfrTooltipApi.ArmorPreviewRequest armorRequest(ItemStack stack, int size) {
        PreviewStyleRegistry.Style style = PreviewStyleRegistry.INSTANCE.match(stack);
        String model = TooltipConfig.zoomOverlayArmorModel;
        String outfit = TooltipConfig.zoomOverlayArmorMode;
        if ("follow".equals(model)) model = style == null ? TooltipConfig.armorPreviewModel
                : style.armorModel(TooltipConfig.armorPreviewModel);
        if ("follow".equals(outfit)) outfit = style == null ? TooltipConfig.armorPreviewMode
                : style.armorMode(TooltipConfig.armorPreviewMode);
        return PreviewEquipment.request(stack, model, outfit, size * 0.38F,
                style == null ? 15 : style.pitch(15), spinSpeed(), size, size,
                Collections.emptyList(), null);
    }

    private static float spinSpeed() {
        return TooltipConfig.zoomOverlayRotation && "spin".equals(TooltipConfig.zoomOverlayMotion)
                ? TooltipConfig.zoomOverlayRotationSpeed : 0;
    }

    private static void renderModel(ItemStack stack, ItemZoomPresentation.Category category, int size) {
        PreviewModelBounds bounds = TooltipConfig.zoomOverlayMeasureBounds
                ? PreviewBoundsMeasurement.zoomItem(stack, category, size) : null;
        if (bounds != null) PreviewBoundsMeasurement.applyFit(bounds, size, size, Math.max(2, size / 25));
        renderModelRaw(stack, category, size,
                TooltipPreviewRenderers.rotationAngle(System.nanoTime(), spinSpeed()));
    }

    static float baseModelScale(int size) {
        return size * 0.88F / (2 * 0.8660254F);
    }

    static void renderModelRaw(ItemStack stack, ItemZoomPresentation.Category category, int size, float spin) {
        PreviewStyleRegistry.Style style = PreviewStyleRegistry.INSTANCE.match(stack);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.translate(size * 0.5F, size * 0.5F, 500);
        net.minecraftforge.client.ForgeHooksClient.multiplyCurrentGlMatrix(modelTransform(
                stack, category, style, baseModelScale(size), spin));
        TooltipPreviewRenderers.renderItemModel(stack);
    }

    /** The exact draw transform, also checked without an OpenGL context in pose regressions. */
    static Matrix4f modelTransform(ItemStack stack, ItemZoomPresentation.Category category,
                                   PreviewStyleRegistry.Style style, float scale, float spin) {
        float pitch = ItemZoomPresentation.pitch(category);
        float roll = ItemZoomPresentation.roll(category);
        // Block orientation is always upright. Item styles remain useful for weapons/shields.
        if (category != ItemZoomPresentation.Category.BLOCK && style != null) {
            // Tooltip styles rotate before their Y reflection. This path reflects first:
            // S * Rx(-pitch) * Ry(yaw) * Rz(-roll) == Rx(pitch) * Ry(yaw) * Rz(roll) * S.
            pitch = -style.pitch(-pitch);
            roll = -style.roll(-roll);
        }
        roll = TooltipPreviewRenderers.itemModelRoll(stack, roll);
        Matrix4f transform = new Matrix4f();
        transform.setIdentity();
        transform.m00 = scale;
        transform.m11 = -scale;
        transform.m22 = scale;
        Matrix4f rotation = new Matrix4f();
        rotation.rotX((float) Math.toRadians(pitch));
        transform.mul(rotation);
        rotation.rotY((float) Math.toRadians(ItemZoomPresentation.yaw(category, spin)));
        transform.mul(rotation);
        rotation.rotZ((float) Math.toRadians(roll));
        transform.mul(rotation);
        return transform;
    }

    static float radius(IBakedModel model) {
        // Tile-entity item renderers have no baked vertices. Reserve a conservative volume.
        if (model.isBuiltInRenderer()) return 1.25F;
        Float cached = RADII.get(model);
        if (cached != null) return cached;
        float radius = 0;
        try {
            Pair<? extends IBakedModel, Matrix4f> perspective =
                    model.handlePerspective(ItemCameraTransforms.TransformType.NONE);
            IBakedModel posed = perspective.getLeft();
            Matrix4f transform = perspective.getRight();
            radius = quadRadius(posed.getQuads(null, null, 0), transform, radius);
            for (EnumFacing face : EnumFacing.values()) {
                radius = quadRadius(posed.getQuads(null, face, 0), transform, radius);
            }
        } catch (RuntimeException | LinkageError ignored) {
            radius = 0;
        }
        radius = Float.isFinite(radius) && radius > 0.01F ? radius : 0.8660254F;
        if (RADII.size() >= 256) RADII.clear();
        RADII.put(model, radius);
        return radius;
    }

    private static float quadRadius(List<BakedQuad> quads, Matrix4f transform, float radius) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertexData();
            int stride = quad.getFormat().getIntegerSize();
            if (stride < 3) continue;
            for (int i = 0; i + 2 < data.length; i += stride) {
                Vector4f point = new Vector4f(Float.intBitsToFloat(data[i]) - 0.5F,
                        Float.intBitsToFloat(data[i + 1]) - 0.5F,
                        Float.intBitsToFloat(data[i + 2]) - 0.5F, 1);
                if (transform != null) transform.transform(point);
                radius = Math.max(radius, (float) Math.sqrt(point.x * point.x
                        + point.y * point.y + point.z * point.z));
            }
        }
        return radius;
    }

    static void clearModelCache() {
        RADII.clear();
    }
}
