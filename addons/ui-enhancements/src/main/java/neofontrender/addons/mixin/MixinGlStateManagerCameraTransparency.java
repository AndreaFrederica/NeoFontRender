package neofontrender.addons.mixin;

import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.camera.CameraRuntime;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps local-player alpha active across model, armor, held-item, and glint render state changes. */
@Mixin(GlStateManager.class)
public abstract class MixinGlStateManagerCameraTransparency {
    private static boolean nfrUi$updatingBlend;

    @ModifyVariable(method = "color(FFFF)V", at = @At("HEAD"), argsOnly = true,
            ordinal = 3, require = 1)
    private static float nfrUi$clampPlayerAlpha(float alpha) {
        return CameraRuntime.isShoulderTransparencyRenderActive()
                ? Math.min(alpha, CameraRuntime.shoulderPlayerAlpha()) : alpha;
    }

    @ModifyVariable(method = "depthMask(Z)V", at = @At("HEAD"), argsOnly = true,
            ordinal = 0, require = 1)
    private static boolean nfrUi$keepTransparentPlayerDepth(boolean enabled) {
        return enabled || CameraRuntime.isShoulderTransparencyRenderActive();
    }

    @Inject(method = "disableBlend()V", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nfrUi$keepTransparentPlayerBlend(CallbackInfo ci) {
        if (CameraRuntime.isShoulderTransparencyRenderActive()) ci.cancel();
    }

    @Inject(method = "blendFunc(II)V", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nfrUi$replaceOpaqueBlend(int sourceFactor, int destinationFactor,
                                                  CallbackInfo ci) {
        if (!CameraRuntime.isShoulderTransparencyRenderActive()
                || sourceFactor != GL11.GL_ONE || destinationFactor != GL11.GL_ZERO) return;
        nfrUi$updatingBlend = true;
        try {
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
        } finally {
            nfrUi$updatingBlend = false;
        }
        ci.cancel();
    }

    @Inject(method = "tryBlendFuncSeparate(IIII)V", at = @At("HEAD"),
            cancellable = true, require = 1)
    private static void nfrUi$preserveTransparentPlayerBlend(int sourceFactor,
                                                              int destinationFactor,
                                                              int sourceAlpha,
                                                              int destinationAlpha,
                                                              CallbackInfo ci) {
        if (!nfrUi$updatingBlend && CameraRuntime.isShoulderTransparencyRenderActive()) ci.cancel();
    }
}
