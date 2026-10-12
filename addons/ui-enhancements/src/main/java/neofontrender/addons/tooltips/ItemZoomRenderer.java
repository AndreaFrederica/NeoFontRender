package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.vecmath.Matrix4f;
import java.nio.FloatBuffer;
import java.util.Collections;

/** Draws into ItemZoomRenderTarget in GUI pixels, independent of tooltip layout and animation. */
final class ItemZoomRenderer {
    private static final ThreadLocal<FloatBuffer> MATRIX =
            ThreadLocal.withInitial(() -> BufferUtils.createFloatBuffer(16));

    private ItemZoomRenderer() {}

    static void render(ItemStack stack, ItemZoomPresentation.Category category, String mode, int size) {
        boolean sway = TooltipConfig.zoomOverlayRotation && "sway".equals(TooltipConfig.zoomOverlayMotion);
        GL11.glPushMatrix();
        try {
            if (sway) {
                GL11.glTranslatef(size * 0.5F, size * 0.5F, 0.0F);
                GL11.glRotatef((float) Math.sin(System.nanoTime() / 800_000_000.0D) * 2.0F,
                        0.0F, 0.0F, 1.0F);
                GL11.glTranslatef(-size * 0.5F, -size * 0.5F, 0.0F);
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
            GL11.glPopMatrix();
        }
    }

    static void renderIcon(ItemStack stack, int size) {
        RenderItem renderer = RenderItem.getInstance();
        Minecraft minecraft = Minecraft.getMinecraft();
        FontRenderer font = minecraft.fontRenderer;
        float oldZ = renderer.zLevel;
        GL11.glPushMatrix();
        try {
            RenderHelper.enableGUIStandardItemLighting();
            // Margin also accommodates the optional 2-degree sway.
            GL11.glTranslatef(size * 0.06F, size * 0.06F, 0.0F);
            GL11.glScalef(size * 0.88F / 16.0F, size * 0.88F / 16.0F, 1.0F);
            renderer.zLevel = 0.0F;
            renderer.renderItemAndEffectIntoGUI(
                    font, minecraft.getTextureManager(), stack, 0, 0);
        } finally {
            renderer.zLevel = oldZ;
            GL11.glPopMatrix();
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
        GL11.glTranslatef(size * 0.5F, size * 0.5F, 500.0F);
        multiplyCurrentGlMatrix(modelTransform(stack, category, style, baseModelScale(size), spin));
        TooltipPreviewRenderers.renderItemModel(stack);
    }

    /**
     * Applies a javax.vecmath transform to the current matrix. 1.7.10's ForgeHooksClient has no
     * matrix helper, so the row-major matrix is uploaded as the equivalent column-major GL matrix.
     */
    private static void multiplyCurrentGlMatrix(Matrix4f matrix) {
        FloatBuffer buffer = MATRIX.get();
        buffer.clear();
        buffer.put(matrix.m00).put(matrix.m10).put(matrix.m20).put(matrix.m30);
        buffer.put(matrix.m01).put(matrix.m11).put(matrix.m21).put(matrix.m31);
        buffer.put(matrix.m02).put(matrix.m12).put(matrix.m22).put(matrix.m32);
        buffer.put(matrix.m03).put(matrix.m13).put(matrix.m23).put(matrix.m33);
        buffer.flip();
        GL11.glMultMatrix(buffer);
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

    /**
     * Drops the cached model measurements after a resource reload. Main also cleared its per-model
     * baked-vertex radius cache here; 1.7.10's item pipeline exposes no baked vertices, so the
     * shared measurement cache is the only model state this renderer can still hold.
     */
    static void clearModelCache() {
        PreviewBoundsMeasurement.clear();
    }
}
