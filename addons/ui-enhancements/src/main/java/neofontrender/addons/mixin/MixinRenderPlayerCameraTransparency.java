package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import neofontrender.addons.camera.CameraRuntime;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

/**
 * Scoped local-player alpha for Shoulder presentation; never leaks GL state.
 *
 * <p>1.7.10 has no GlStateManager, so the scoped state goes straight to the fixed-function
 * pipeline. This class opens and closes the scope around the local player's render;
 * {@link MixinRenderLivingBaseCameraViewIdentity} re-applies the scoped alpha where 1.7.10's own
 * colour and blend resets would otherwise drop it.
 *
 * <p>1.12.2 keeps the model in the world by redirecting {@code AbstractClientPlayer#isUser} inside
 * {@code RenderPlayer#doRender}. 1.7.10 has neither that method nor that call: it hides the local
 * player one level up in {@code RenderGlobal#renderEntities} ({@code entity != renderViewEntity ||
 * thirdPersonView != 0}), which already renders the player while a detached camera keeps a
 * non-zero third-person view. Forcing the player in for a first-person-keyed anchored rig would
 * need a RenderGlobal hook, which this addon's mixin set does not declare.
 */
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

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void nfrUi$beginPlayerAlpha(AbstractClientPlayer player, double x, double y,
                                         double z, float yaw, float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player == minecraft.thePlayer && CameraRuntime.shoulderSkipPlayerRendering()) {
            ci.cancel();
            return;
        }
        if (player != minecraft.thePlayer || !CameraRuntime.shoulderPlayerTransparency()) return;
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
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(true);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, CameraRuntime.shoulderPlayerAlpha());
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("RETURN"), require = 1)
    private void nfrUi$endPlayerAlpha(AbstractClientPlayer player, double x, double y,
                                       double z, float yaw, float partialTicks, CallbackInfo ci) {
        if (!nfrUi$alphaApplied) return;
        nfrUi$alphaApplied = false;
        CameraRuntime.endShoulderTransparencyRender();
        GL11.glColor4f(nfrUi$color.get(0), nfrUi$color.get(1),
                nfrUi$color.get(2), nfrUi$color.get(3));
        GL11.glAlphaFunc(nfrUi$alphaFunc, nfrUi$alphaRef);
        GL14.glBlendFuncSeparate(nfrUi$blendSrcRgb, nfrUi$blendDstRgb,
                nfrUi$blendSrcAlpha, nfrUi$blendDstAlpha);
        GL11.glDepthMask(nfrUi$depthMask);
        if (nfrUi$blendEnabled) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        if (nfrUi$alphaEnabled) GL11.glEnable(GL11.GL_ALPHA_TEST);
        else GL11.glDisable(GL11.GL_ALPHA_TEST);
    }
}
