package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import neofontrender.addons.flight.FlightRollRenderState;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightAttitude;
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
        if (Minecraft.getMinecraft().player != player
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

    @Inject(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At("HEAD"), cancellable = true)
    private void nfrUi$applyLongitudinalRoll(AbstractClientPlayer player, float ageInTicks,
                                              float rotationYaw, float partialTicks,
                                              CallbackInfo ci) {
        if (!player.isElytraFlying()) return;
        FlightBodyPose bodyPose = FlightApi.queryBodyPose(player, partialTicks);
        float roll = FlightRollRenderState.roll(player, partialTicks);
        if (bodyPose == null && Math.abs(roll) <= 0.001F) return;

        if (bodyPose != null) {
            // Apply the complete aircraft quaternion directly.  The fixed base transform maps
            // Minecraft's upright model (+Y head axis) onto aircraft-local +Z forward.
            FlightAttitude attitude = bodyPose.attitude;
            double clampedW = Math.max(-1.0D, Math.min(1.0D, attitude.w));
            double halfSine = Math.sqrt(Math.max(0.0D, 1.0D - clampedW * clampedW));
            if (halfSine > 1.0E-9D) {
                GlStateManager.rotate((float) Math.toDegrees(2.0D * Math.acos(clampedW)),
                        (float) (attitude.x / halfSine),
                        (float) (attitude.y / halfSine),
                        (float) (attitude.z / halfSine));
            }
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
            ci.cancel();
            return;
        }

        // Vanilla's final motion-yaw correction keeps the body upright and therefore fights a
        // full barrel roll. Rebuild the flying pose in aircraft-local order, matching the modern
        // DABR player renderer: view yaw, elytra pitch transition, then longitudinal body Y roll.
        float bodyYaw;
        float bodyPitch;
        float flightProgress;
        bodyYaw = nfrUi$interpolateAngle(player.prevRotationYaw,
                player.rotationYaw, partialTicks);
        bodyPitch = player.prevRotationPitch
                + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float flightTicks = player.getTicksElytraFlying() + partialTicks;
        flightProgress = Math.max(0.0F, Math.min(1.0F,
                flightTicks * flightTicks / 100.0F));
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(flightProgress * (-90.0F - bodyPitch), 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(roll, 0.0F, 1.0F, 0.0F);
        ci.cancel();
    }

    /** Generic vehicle/custom-flight path: vanilla pose first, then roll around model-forward Z. */
    @Inject(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At("RETURN"))
    private void nfrUi$applyNonElytraLongitudinalRoll(AbstractClientPlayer player,
                                                       float ageInTicks, float rotationYaw,
                                                       float partialTicks, CallbackInfo ci) {
        if (player.isElytraFlying()) return;
        float roll = FlightRollRenderState.roll(player, partialTicks);
        if (Math.abs(roll) > 0.001F) GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
    }

    @Unique
    private static float nfrUi$interpolateAngle(float previous, float current, float partialTicks) {
        float delta = current - previous;
        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;
        return previous + Math.max(0.0F, Math.min(1.0F, partialTicks)) * delta;
    }
}
