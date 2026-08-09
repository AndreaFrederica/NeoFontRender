package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.flight.FlightRollRenderState;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies synchronized roll on the player's local longitudinal axis in third person. */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayerFlightRoll {
    @Unique private boolean nfrUi$poseOverridden;
    @Unique private float nfrUi$previousBodyYaw;
    @Unique private float nfrUi$bodyYaw;
    @Unique private float nfrUi$previousHeadYaw;
    @Unique private float nfrUi$headYaw;

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("HEAD"))
    private void nfrUi$alignLocalFlightPose(AbstractClientPlayer player, double x, double y,
                                             double z, float entityYaw, float partialTicks,
                                             CallbackInfo ci) {
        nfrUi$poseOverridden = false;
        if (FlightApi.queryBodyPose(player, partialTicks) != null) return;
        if (Minecraft.getMinecraft().thePlayer != player
                || Math.abs(FlightRollRenderState.roll(player, partialTicks)) < 0.001F) return;
        nfrUi$poseOverridden = true;
        nfrUi$previousBodyYaw = player.prevRenderYawOffset;
        nfrUi$bodyYaw = player.renderYawOffset;
        nfrUi$previousHeadYaw = player.prevRotationYawHead;
        nfrUi$headYaw = player.rotationYawHead;
        player.prevRenderYawOffset = player.prevRotationYaw;
        player.renderYawOffset = player.rotationYaw;
        player.prevRotationYawHead = player.prevRotationYaw;
        player.rotationYawHead = player.rotationYaw;
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            at = @At("RETURN"))
    private void nfrUi$restoreLocalFlightPose(AbstractClientPlayer player, double x, double y,
                                               double z, float entityYaw, float partialTicks,
                                               CallbackInfo ci) {
        if (!nfrUi$poseOverridden) return;
        player.prevRenderYawOffset = nfrUi$previousBodyYaw;
        player.renderYawOffset = nfrUi$bodyYaw;
        player.prevRotationYawHead = nfrUi$previousHeadYaw;
        player.rotationYawHead = nfrUi$headYaw;
        nfrUi$poseOverridden = false;
    }

    /** Custom-flight path: vanilla pose first, then roll around model-forward Z. */
    @Inject(method = "rotateCorpse(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At("RETURN"))
    private void nfrUi$applyLongitudinalRoll(AbstractClientPlayer player,
                                             float ageInTicks, float rotationYaw,
                                             float partialTicks, CallbackInfo ci) {
        FlightBodyPose bodyPose = FlightApi.queryBodyPose(player, partialTicks);
        float roll = FlightRollRenderState.roll(player, partialTicks);
        if (bodyPose == null && Math.abs(roll) <= 0.001F) return;
        if (bodyPose != null) {
            FlightAttitude attitude = bodyPose.attitude;
            double clampedW = Math.max(-1.0D, Math.min(1.0D, attitude.w));
            double halfSine = Math.sqrt(Math.max(0.0D, 1.0D - clampedW * clampedW));
            if (halfSine > 1.0E-9D) {
                GL11.glRotatef((float) Math.toDegrees(2.0D * Math.acos(clampedW)),
                        (float) (attitude.x / halfSine),
                        (float) (attitude.y / halfSine),
                        (float) (attitude.z / halfSine));
            }
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
            ci.cancel();
            return;
        }
        if (Math.abs(roll) > 0.001F) GL11.glRotatef(roll, 0.0F, 0.0F, 1.0F);
    }

}
