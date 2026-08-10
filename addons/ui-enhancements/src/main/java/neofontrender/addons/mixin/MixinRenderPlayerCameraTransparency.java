package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import neofontrender.addons.camera.CameraRuntime;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scoped local-player alpha for Shoulder presentation; never leaks GL state. */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayerCameraTransparency {
    private boolean nfrUi$alphaApplied;
    private boolean nfrUi$blendEnabled;
    private boolean nfrUi$alphaEnabled;
    private boolean nfrUi$depthMask;
    private int nfrUi$alphaFunc;
    private float nfrUi$alphaRef;
    private int nfrUi$blendSrcRgb, nfrUi$blendDstRgb, nfrUi$blendSrcAlpha, nfrUi$blendDstAlpha;
    private final FloatBuffer nfrUi$color = BufferUtils.createFloatBuffer(4);

    /** Keep the player model in the world for player-anchored custom third-person rigs. */
    @Redirect(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/client/entity/AbstractClientPlayer;isUser()Z"), require = 1)
    private boolean nfrUi$renderLocalPlayerForCameraMode(AbstractClientPlayer player) {
        return player.isUser() && !CameraRuntime.shouldRenderDetachedLocalPlayer()
                && !CameraRuntime.isPlayerAnchoredCameraActive();
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$beginPlayerAlpha(AbstractClientPlayer player, double x, double y,
                                         double z, float yaw, float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player == minecraft.player && CameraRuntime.shoulderSkipPlayerRendering()) {
            ci.cancel();
            return;
        }
        if (player == minecraft.player && CameraRuntime.shoulderPlayerTransparency()) {
            nfrUi$alphaApplied = true;
            nfrUi$blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            nfrUi$alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            nfrUi$depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            nfrUi$alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            nfrUi$alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            nfrUi$blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
            nfrUi$blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            nfrUi$blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
            nfrUi$blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            nfrUi$color.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, nfrUi$color);
            CameraRuntime.beginShoulderTransparencyRender();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, CameraRuntime.shoulderPlayerAlpha());
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("RETURN"), require = 1)
    private void nfrUi$endPlayerAlpha(AbstractClientPlayer player, double x, double y,
                                       double z, float yaw, float partialTicks, CallbackInfo ci) {
        if (!nfrUi$alphaApplied) return;
        nfrUi$alphaApplied = false;
        CameraRuntime.endShoulderTransparencyRender();
        GlStateManager.color(nfrUi$color.get(0), nfrUi$color.get(1),
                nfrUi$color.get(2), nfrUi$color.get(3));
        GlStateManager.alphaFunc(nfrUi$alphaFunc, nfrUi$alphaRef);
        GlStateManager.tryBlendFuncSeparate(nfrUi$blendSrcRgb, nfrUi$blendDstRgb,
                nfrUi$blendSrcAlpha, nfrUi$blendDstAlpha);
        GlStateManager.depthMask(nfrUi$depthMask);
        if (nfrUi$blendEnabled) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        if (nfrUi$alphaEnabled) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
    }
}
