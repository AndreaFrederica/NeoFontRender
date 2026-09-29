package neofontrender.addons.tooltips;

import com.google.gson.JsonObject;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector4f;

import static org.junit.jupiter.api.Assertions.*;

class ItemZoomPoseTest {
    @BeforeAll
    static void bootstrap() { net.minecraft.init.Bootstrap.register(); }

    @Test
    void swordBladeStaysUprightForAnEntireTurn() {
        ItemStack sword = new ItemStack(Items.WOODEN_SWORD);
        for (int yaw = 0; yaw < 360; yaw += 15) {
            Matrix4f transform = ItemZoomRenderer.modelTransform(sword,
                    ItemZoomPresentation.Category.TOOL, null, 1, yaw);
            // Generated sword geometry points from the lower-left hilt to the upper-right tip.
            Vector4f blade = new Vector4f(1, 1, 0, 0);
            transform.transform(blade);
            assertEquals(0, blade.x, 0.0001F, "Blade leaned sideways at yaw " + yaw);
            assertTrue(blade.y < -1, "Tip must point upwards in GUI coordinates at yaw " + yaw);
        }
    }

    @Test
    void resourcePackAnglesMatchTooltipDespiteDifferentReflectionOrder() {
        JsonObject json = new JsonObject();
        json.addProperty("pitch", -25);
        json.addProperty("roll", -40);
        PreviewStyleRegistry.Style style = PreviewStyleRegistry.Style.parse(json);
        for (ItemStack stack : new ItemStack[]{new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.SHIELD)}) {
            for (int yaw : new int[]{0, 72, 180, 321}) {
                Matrix4f actual = ItemZoomRenderer.modelTransform(stack,
                        ItemZoomPresentation.Category.TOOL, style, 2, yaw);
                Matrix4f expected = tooltipTransform(-25, yaw,
                        TooltipPreviewRenderers.itemModelRoll(stack, -40), 2);
                assertTrue(expected.epsilonEquals(actual, 0.0001F),
                        "Resource-pack pose differs from tooltip at yaw " + yaw);
            }
        }
    }

    @Test
    void blocksKeepTheirUprightAxisWithToolStylesPresent() {
        JsonObject json = new JsonObject();
        json.addProperty("pitch", -25);
        json.addProperty("roll", -40);
        PreviewStyleRegistry.Style style = PreviewStyleRegistry.Style.parse(json);
        for (int yaw = 0; yaw < 360; yaw += 30) {
            Matrix4f actual = ItemZoomRenderer.modelTransform(ItemStack.EMPTY,
                    ItemZoomPresentation.Category.BLOCK, style, 1, yaw);
            Vector4f upright = new Vector4f(0, 1, 0, 0);
            actual.transform(upright);
            assertEquals(0, upright.x, 0.0001F);
            assertTrue(upright.y < -0.9F);
        }
    }

    private static Matrix4f tooltipTransform(float pitch, float yaw, float roll, float scale) {
        Matrix4f result = new Matrix4f();
        result.rotX((float) Math.toRadians(pitch));
        Matrix4f next = new Matrix4f();
        next.rotY((float) Math.toRadians(yaw));
        result.mul(next);
        next.rotZ((float) Math.toRadians(roll));
        result.mul(next);
        next.setIdentity();
        next.m00 = scale;
        next.m11 = -scale;
        next.m22 = scale;
        result.mul(next);
        return result;
    }
}
