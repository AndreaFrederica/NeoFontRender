package neofontrender.addons.tooltips;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TooltipPreviewRenderersTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        net.minecraft.init.Bootstrap.register();
    }

    @Test
    void initializeRegistersBuiltInBackendsBeforeLayoutMeasurement() {
        TooltipPreviewRenderers.initialize();

        assertNotNull(TooltipPreviewRenderers.find(
                new NfrTooltipApi.ItemPreviewRequest(ItemStack.EMPTY)));
        assertNotNull(TooltipPreviewRenderers.find(
                new NfrTooltipApi.ArmorPreviewRequest(ItemStack.EMPTY,
                        EntityEquipmentSlot.CHEST)));
        assertNotNull(NfrTooltipApi.PreviewRegistry.findEffect("icon_particles"));
    }

    @Test
    void animationIdentityIncludesNbtAndArmorPresentation() {
        Item item = new Item().setRegistryName(new ResourceLocation("test", "preview"));
        ItemStack first = new ItemStack(item);
        ItemStack second = new ItemStack(item);
        second.setCount(32);
        assertEquals(TooltipPreviewRenderers.animationKey(null, first),
                TooltipPreviewRenderers.animationKey(null, second));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("charge", 2);
        second.setTagCompound(tag);

        assertNotEquals(TooltipPreviewRenderers.animationKey(
                        new NfrTooltipApi.ItemPreviewRequest(first), first),
                TooltipPreviewRenderers.animationKey(
                        new NfrTooltipApi.ItemPreviewRequest(second), second));

        NfrTooltipApi.ArmorPreviewRequest stand = new NfrTooltipApi.ArmorPreviewRequest(
                first, EntityEquipmentSlot.CHEST, NfrTooltipApi.ArmorPreviewModel.ARMOR_STAND,
                NfrTooltipApi.ArmorPreviewMode.SINGLE_PIECE, 30.0F, 25.0F, 20.0F);
        NfrTooltipApi.ArmorPreviewRequest player = new NfrTooltipApi.ArmorPreviewRequest(
                first, EntityEquipmentSlot.CHEST, NfrTooltipApi.ArmorPreviewModel.PLAYER,
                NfrTooltipApi.ArmorPreviewMode.SINGLE_PIECE, 30.0F, 25.0F, 20.0F);

        assertNotEquals(TooltipPreviewRenderers.animationKey(stand, first),
                TooltipPreviewRenderers.animationKey(player, first));
    }

    @Test
    void disablingMeasurementPreservesTheConfiguredPreviewCell() {
        TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        try {
            TooltipConfig.previewMeasureBounds = false;
            NfrTooltipApi.ItemPreviewRequest item = new NfrTooltipApi.ItemPreviewRequest(
                    ItemStack.EMPTY, 3, 25, 0, 20, 37, 83, Collections.emptyList());
            NfrTooltipApi.PreviewSize measured = TooltipPreviewRenderers.find(item).measure(item, null);
            assertEquals(37, measured.width());
            assertEquals(83, measured.height());
            NfrTooltipApi.ArmorPreviewRequest armor = new NfrTooltipApi.ArmorPreviewRequest(
                    ItemStack.EMPTY, EntityEquipmentSlot.HEAD);
            measured = TooltipPreviewRenderers.find(armor).measure(armor, null);
            assertEquals(armor.width(), measured.width());
            assertEquals(armor.height(), measured.height());
        } finally {
            original.restore();
        }
    }

    @Test
    void rotationKeepsFramePrecisionAtLongRuntimeValues() {
        long oneThousandHours = 3_600_000_000_000_000L;

        float first = TooltipPreviewRenderers.rotationAngle(
                oneThousandHours + 1_000_000_000L, -20.0F);
        float nextFrame = TooltipPreviewRenderers.rotationAngle(
                oneThousandHours + 1_050_000_000L, -20.0F);

        assertEquals(-20.0F, first, 0.001F);
        assertEquals(-21.0F, nextFrame, 0.001F);
    }

    @Test
    void builtInEffectsDeclareTheirPaintOutsets() {
        TooltipPreviewRenderers.initialize();
        NfrTooltipApi.ItemPreviewRequest request = new NfrTooltipApi.ItemPreviewRequest(
                ItemStack.EMPTY, 1.0F, 0.0F, 0.0F, 0.0F, 30, 64,
                Collections.singletonList("ray_glow"));

        NfrTooltipApi.PreviewInsets outsets = PreviewEffects.outsets(
                request, new NfrTooltipApi.PreviewSize(30, 64));

        assertEquals(8, outsets.left());
        assertEquals(8, outsets.top());
        assertEquals(8, outsets.right());
        assertEquals(8, outsets.bottom());

        NfrTooltipApi.ItemPreviewRequest lines = new NfrTooltipApi.ItemPreviewRequest(
                ItemStack.EMPTY, 1.0F, 0.0F, 0.0F, 0.0F, 30, 64,
                Collections.singletonList("line_particles"));
        NfrTooltipApi.PreviewInsets lineOutsets = PreviewEffects.outsets(
                lines, new NfrTooltipApi.PreviewSize(30, 64));
        assertEquals(8, lineOutsets.left());
        assertEquals(13, lineOutsets.right());
    }
}
