package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restores the one GUI state RPG HUD Lite does not restore: the active texture unit. */
@Pseudo
@Mixin(targets = "com.sao.rpghudlite.CompactHud", remap = false)
public abstract class MixinRpgHudLiteGuiState {
    private int nfrUi$activeTexture;

    @Inject(method = "render", at = @At("HEAD"), remap = false, require = 1)
    private void nfrUi$pushGuiState(Minecraft minecraft,
                                    net.minecraft.client.gui.ScaledResolution resolution,
                                    float partialTicks, CallbackInfo callback) {
        nfrUi$activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false, require = 1)
    private void nfrUi$restoreGuiTextureState(Minecraft minecraft,
                                              net.minecraft.client.gui.ScaledResolution resolution,
                                              float partialTicks, CallbackInfo callback) {
        // CompactHud already restores its own RenderState and model-view matrix. Only the
        // active texture unit is missing from that snapshot. Restore it through GlStateManager
        // so Minecraft's texture-unit cache stays synchronized with the driver.
        GlStateManager.setActiveTexture(nfrUi$activeTexture);
    }
}
