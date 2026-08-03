package neofontrender.addons.mixin;

import net.minecraft.client.LoadingScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.tips.TipManager;
import neofontrender.addons.tips.TipRenderer;
import neofontrender.addons.tips.TipsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders tips on Forge's vanilla loading screen (LoadingScreenRenderer).
 */
@Mixin(LoadingScreenRenderer.class)
public abstract class MixinLoadingScreenRendererTips {
    @Shadow private Minecraft mc;

    @Inject(method = "setLoadingProgress", at = @At("TAIL"))
    private void nfrUi$renderTips(int progress, CallbackInfo ci) {
        if (!TipsConfig.enabled || !TipsConfig.showOnForgeLoading) return;
        if (mc.currentScreen != null) return;

        long now = System.nanoTime();
        TipManager.INSTANCE.update(now, TipsConfig.cycleTimeMillis);

        int width = mc.displayWidth;
        int height = mc.displayHeight;
        int margin = Math.max(12, Math.min(28, width / 32));

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 0);
        TipRenderer.draw(width, height, margin, height / 2, 1.0F, 0xFFFFFFFF);
        GlStateManager.popMatrix();
    }
}
