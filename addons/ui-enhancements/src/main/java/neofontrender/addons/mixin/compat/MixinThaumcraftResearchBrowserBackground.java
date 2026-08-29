package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import neofontrender.addons.effects.ScreenEffectsRenderer;

/** Prevents TC6's legacy depth-equal pass from rejecting the research background. */
@Pseudo
@Mixin(targets = "thaumcraft.client.gui.GuiResearchBrowser", remap = false)
public abstract class MixinThaumcraftResearchBrowserBackground {
    @Inject(method = "genResearchBackgroundFixedPre", at = @At("RETURN"),
            require = 0, remap = false)
    private void nfrUi$disableLegacyDepthEqual(int mouseX, int mouseY, float partialTicks,
                                                int guiLeft, int guiTop, CallbackInfo ci) {
        if (!ScreenEffectsRenderer.INSTANCE.drawBackground((GuiScreen) (Object) this)) return;
        GlStateManager.disableDepth();
    }
}
