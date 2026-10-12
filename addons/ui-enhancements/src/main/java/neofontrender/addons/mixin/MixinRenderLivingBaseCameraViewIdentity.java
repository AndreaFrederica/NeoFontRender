package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import neofontrender.addons.camera.CameraRuntime;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores the vanilla view-entity identity rule that upstream Shoulder Surfing preserves.
 *
 * <p>1.7.10 has no RenderLivingBase and no GlStateManager: {@code RendererLivingEntity} is the one
 * target that owns both the nametag identity check ({@code canRenderName}) and the in-place colour
 * and blend resets of the model, armor, and held-item passes. The scoped local-player alpha that
 * {@link MixinRenderPlayerCameraTransparency} opens is therefore re-applied here.
 *
 * <p>The {@code glColor4f}/{@code glDisable}/{@code glBlendFunc} redirects are the counterpart of
 * 1.12.2's GlStateManager interceptions. They target fixed-function calls inside this class, so a
 * platform transformer that rewrites GL calls before Mixin (Angelica's or Actinium's GL state
 * manager) can leave them unmatched; they deliberately omit {@code require} so that such a build
 * only loses the extra armor/held-item alpha continuity and logs a warning instead of failing.
 */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRenderLivingBaseCameraViewIdentity {
    @Inject(method = "func_110813_b(Lnet/minecraft/entity/EntityLivingBase;)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$restoreDetachedViewEntityIdentity(EntityLivingBase entity,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (entity == Minecraft.getMinecraft().thePlayer
                && CameraRuntime.shouldRenderDetachedLocalPlayer()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Re-applies the scoped alpha at the model boundary: 1.7.10 resets the current colour on the
     * way in, so the alpha cannot survive from RenderPlayer alone.
     */
    @Inject(method = "renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V",
            at = @At("HEAD"), require = 1)
    private void nfrUi$applyScopedPlayerAlpha(EntityLivingBase entity, float limbSwing,
                                               float limbSwingAmount, float ageInTicks,
                                               float netHeadYaw, float headPitch, float scale,
                                               CallbackInfo ci) {
        if (CameraRuntime.isShoulderTransparencyRenderActive()) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, CameraRuntime.shoulderPlayerAlpha());
        }
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V",
                    remap = false))
    private void nfrUi$clampScopedPlayerAlpha(float red, float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, nfrUi$scopedAlpha(alpha));
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V",
                    remap = false))
    private void nfrUi$keepScopedPlayerBlend(int capability) {
        if (capability == GL11.GL_BLEND && CameraRuntime.isShoulderTransparencyRenderActive()) return;
        GL11.glDisable(capability);
    }

    @Redirect(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBlendFunc(II)V",
                    remap = false))
    private void nfrUi$keepScopedPlayerBlendFunc(int sourceFactor, int destinationFactor) {
        if (!CameraRuntime.isShoulderTransparencyRenderActive()) {
            GL11.glBlendFunc(sourceFactor, destinationFactor);
            return;
        }
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
    }

    private static float nfrUi$scopedAlpha(float alpha) {
        return CameraRuntime.isShoulderTransparencyRenderActive()
                ? Math.min(alpha, CameraRuntime.shoulderPlayerAlpha()) : alpha;
    }
}
