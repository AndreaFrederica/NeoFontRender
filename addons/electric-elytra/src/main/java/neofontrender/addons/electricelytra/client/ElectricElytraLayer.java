package neofontrender.addons.electricelytra.client;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelElytra;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.electricelytra.ItemElectricElytra;

final class ElectricElytraLayer implements LayerRenderer<AbstractClientPlayer> {
    private static final ResourceLocation VANILLA_ELYTRA =
            new ResourceLocation("textures/entity/elytra.png");
    private final ModelElytra model = new ModelElytra();

    ElectricElytraLayer(RenderPlayer renderer) {}

    @Override public void doRenderLayer(AbstractClientPlayer player, float limbSwing,
                                        float limbSwingAmount, float partialTicks,
                                        float ageInTicks, float netHeadYaw, float headPitch,
                                        float scale) {
        ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (!ItemElectricElytra.isElectricElytra(stack)) return;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        ResourceLocation texture = VANILLA_ELYTRA;
        if (player.isPlayerInfoSet() && player.getLocationElytra() != null) {
            texture = player.getLocationElytra();
        } else if (player.hasPlayerInfo() && player.getLocationCape() != null
                && player.isWearing(EnumPlayerModelParts.CAPE)) {
            texture = player.getLocationCape();
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.125F);
        model.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale, player);
        model.render(player, limbSwing, limbSwingAmount, ageInTicks,
                netHeadYaw, headPitch, scale);
        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }

    @Override public boolean shouldCombineTextures() { return false; }
}
