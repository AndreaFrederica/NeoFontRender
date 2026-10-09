package neofontrender.addons.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.flight.FlightBodyPresentationPolicy;
import neofontrender.addons.flight.FlightRollRenderState;
import neofontrender.addons.flight.PlayerBodyRenderScope;
import neofontrender.addons.flight.PlayerBodyRenderTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies one sampled body pose to the third-person player root and relative head. */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayerFlightRoll {
    // The dev target is MCP-mapped, while the installed Cleanroom target is SRG-mapped.
    // Unimined can omit this MixinExtras selector from the release refmap, so keep both exact
    // method names and bypass remapping for this selector only.
    @WrapMethod(method = {
            "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
            "func_76986_a(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"
    }, remap = false, require = 1, expect = 1, allow = 1)
    private void nfrUi$renderWithSampledBodyPose(AbstractClientPlayer player,
                                                 double x, double y, double z,
                                                 float entityYaw, float partialTicks,
                                                 Operation<Void> original) throws Throwable {
        float[] previous = nfrUi$snapshot(player);
        try {
            PlayerBodyRenderScope.withSampledPose(player, () -> {
                nfrUi$applyCursorPose(player, partialTicks);
                if (!nfrUi$mayOwnRoot(player)) return null;
                return FlightApi.queryBodyPose(player, partialTicks);
            }, pose -> FlightBodyPresentationPolicy.resolve(pose, player.isElytraFlying())
                    .ownsRootQuaternion(), () -> {
                PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current(player);
                if (context != null && context.active()) {
                    nfrUi$applySampledHead(player, context.transform());
                } else {
                    boolean localPlayer = Minecraft.getMinecraft().player == player;
                    float roll = localPlayer && (context == null || !context.hasPose())
                            ? FlightRollRenderState.roll(player, partialTicks) : 0.0F;
                    float[] aligned = nfrUi$snapshot(player);
                    if (PlayerBodyRenderScope.alignLocalRollWhenPoseAbsent(context,
                            localPlayer, roll, player.prevRotationYaw, player.rotationYaw, aligned)) {
                        nfrUi$restore(player, aligned);
                    }
                }
                original.call(player, x, y, z, entityYaw, partialTicks);
            });
        } finally {
            nfrUi$restore(player, previous);
        }
    }

    @Inject(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At("HEAD"), cancellable = true)
    private void nfrUi$applyLongitudinalRoll(AbstractClientPlayer player, float ageInTicks,
                                              float rotationYaw, float partialTicks,
                                              CallbackInfo ci) {
        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current(player);
        if (context != null && context.active()) {
            nfrUi$rotate(context.transform().ysmRootAttitude());
            ci.cancel();
            return;
        }
        if (!player.isElytraFlying()) return;
        float roll = FlightRollRenderState.roll(player, partialTicks);
        if (Math.abs(roll) <= 0.001F) return;

        // Keep the established electric-elytra fallback when no body-pose provider owns the root.
        float bodyYaw = nfrUi$interpolateAngle(
                player.prevRotationYaw, player.rotationYaw, partialTicks);
        float bodyPitch = player.prevRotationPitch
                + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float flightTicks = player.getTicksElytraFlying() + partialTicks;
        float flightProgress = Math.max(0.0F, Math.min(1.0F,
                flightTicks * flightTicks / 100.0F));
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(flightProgress * (-90.0F - bodyPitch), 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(roll, 0.0F, 1.0F, 0.0F);
        ci.cancel();
    }

    @Inject(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
            at = @At("RETURN"))
    private void nfrUi$applyNonElytraLongitudinalRoll(AbstractClientPlayer player,
                                                       float ageInTicks, float rotationYaw,
                                                       float partialTicks, CallbackInfo ci) {
        if (player.isElytraFlying()) return;
        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current(player);
        if (context != null && context.active()) return;
        float roll = FlightRollRenderState.roll(player, partialTicks);
        if (Math.abs(roll) > 0.001F) GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
    }

    @Unique
    private static void nfrUi$applyCursorPose(AbstractClientPlayer player, float partialTicks) {
        if (Minecraft.getMinecraft().player != player
                || !neofontrender.addons.camera.CameraRuntime.isFreeLookCursorMode()) return;
        float[] pose = neofontrender.addons.camera.CameraRuntime.freeLookCursorPose(partialTicks);
        if (pose == null) return;
        player.prevRotationPitch = pose[1];
        player.rotationPitch = pose[1];
        player.prevRotationYawHead = pose[0];
        player.rotationYawHead = pose[0];
        player.prevRenderYawOffset = pose[2];
        player.renderYawOffset = pose[2];
    }

    @Unique
    private static float[] nfrUi$snapshot(AbstractClientPlayer player) {
        return new float[]{player.prevRotationPitch, player.rotationPitch,
                player.prevRotationYawHead, player.rotationYawHead,
                player.prevRenderYawOffset, player.renderYawOffset};
    }

    @Unique
    private static void nfrUi$restore(AbstractClientPlayer player, float[] previous) {
        player.prevRotationPitch = previous[0];
        player.rotationPitch = previous[1];
        player.prevRotationYawHead = previous[2];
        player.rotationYawHead = previous[3];
        player.prevRenderYawOffset = previous[4];
        player.renderYawOffset = previous[5];
    }

    @Unique
    private static void nfrUi$applySampledHead(AbstractClientPlayer player,
                                                PlayerBodyRenderTransform transform) {
        PlayerBodyRenderTransform.MinecraftLocalAngles angles = transform.minecraftLocalAngles();
        player.prevRotationPitch = (float) angles.pitchDegrees;
        player.rotationPitch = (float) angles.pitchDegrees;
        player.prevRotationYawHead = player.prevRenderYawOffset + (float) angles.yawDegrees;
        player.rotationYawHead = player.renderYawOffset + (float) angles.yawDegrees;
    }

    @Unique
    private static boolean nfrUi$mayOwnRoot(AbstractClientPlayer player) {
        return player.isEntityAlive() && !player.isPlayerSleeping() && !player.isRiding();
    }

    @Unique
    private static void nfrUi$rotate(FlightAttitude attitude) {
        double sign = attitude.w < 0.0D ? -1.0D : 1.0D;
        double w = sign * attitude.w;
        double x = sign * attitude.x;
        double y = sign * attitude.y;
        double z = sign * attitude.z;
        double halfSine = Math.sqrt(Math.max(0.0D, 1.0D - w * w));
        if (halfSine <= 1.0E-9D) return;
        float degrees = (float) Math.toDegrees(2.0D * Math.atan2(halfSine, w));
        GlStateManager.rotate(degrees,
                (float) (x / halfSine), (float) (y / halfSine), (float) (z / halfSine));
    }

    @Unique
    private static float nfrUi$interpolateAngle(float previous, float current, float partialTicks) {
        float delta = current - previous;
        while (delta < -180.0F) delta += 360.0F;
        while (delta >= 180.0F) delta -= 360.0F;
        return previous + Math.max(0.0F, Math.min(1.0F, partialTicks)) * delta;
    }
}
