package neofontrender.addons.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.compat.YsmPoseConfig;
import neofontrender.addons.flight.FlightBodyPresentationPolicy;
import neofontrender.addons.flight.PlayerBodyRenderScope;
import neofontrender.addons.flight.PlayerBodyRenderTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Optional YSM 1.1.9 root/head bridge. No YSM types are linked from UIE bytecode. */
@Pseudo
@Mixin(targets = "com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer",
        remap = false)
abstract class MixinYsmPlayerBodyPose {
    @WrapMethod(method = "render(Lnet/minecraft/entity/EntityLivingBase;"
            + "Lcom/elfmcys/yesstevemodel/geckolib3/core/IAnimatable;DDDFF)V",
            require = 1, expect = 1, allow = 1)
    private void nfrUi$renderWithSampledBodyPose(EntityLivingBase entity,
                                                  @Coerce Object animatable,
                                                  double x, double y, double z,
                                                  float entityYaw, float partialTicks,
                                                  Operation<Void> original) throws Throwable {
        PlayerBodyRenderScope.withSampledPose(entity, () -> {
            if (!(entity instanceof AbstractClientPlayer player)
                    || !YsmPoseConfig.enabled()
                    || !nfrUi$mayOwnRoot(player)) return null;
            return FlightApi.queryBodyPose(player, partialTicks);
        }, pose -> entity instanceof AbstractClientPlayer player
                && FlightBodyPresentationPolicy.resolve(pose, player.isElytraFlying())
                .ownsRootQuaternion(),
                () -> original.call(entity, animatable, x, y, z, entityYaw, partialTicks));
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/EntityLivingBase;"
                    + "Lcom/elfmcys/yesstevemodel/geckolib3/core/IAnimatable;DDDFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/elfmcys/yesstevemodel/geckolib3/geo/GeoReplacedEntityRenderer;"
                            + "func_77043_a(Lnet/minecraft/entity/EntityLivingBase;FFF)V"),
            require = 1, expect = 1, allow = 1)
    private void nfrUi$replaceRootRotation(@Coerce Object renderer,
                                            EntityLivingBase entity,
                                            float ageInTicks, float rotationYaw,
                                            float partialTicks,
                                            Operation<Void> original) {
        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current(entity);
        if (context == null || !context.active()) {
            original.call(renderer, entity, ageInTicks, rotationYaw, partialTicks);
            return;
        }
        nfrUi$rotate(context.transform().ysmRootAttitude());
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/EntityLivingBase;"
                    + "Lcom/elfmcys/yesstevemodel/geckolib3/core/IAnimatable;DDDFF)V",
            at = @At(value = "FIELD",
                    target = "Lcom/elfmcys/yesstevemodel/geckolib3/model/provider/data/"
                            + "EntityModelData;headPitch:F",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD),
            require = 1, expect = 1, allow = 1)
    private void nfrUi$replaceHeadPitch(@Coerce Object modelData, float originalPitch,
                                        Operation<Void> original) {
        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current();
        if (context == null || !context.active()) {
            original.call(modelData, originalPitch);
            return;
        }
        PlayerBodyRenderTransform.YsmBoneAngles angles = context.transform().ysmBoneAngles();
        original.call(modelData, (float) angles.pitchDegrees);
    }

    @WrapOperation(
            method = "render(Lnet/minecraft/entity/EntityLivingBase;"
                    + "Lcom/elfmcys/yesstevemodel/geckolib3/core/IAnimatable;DDDFF)V",
            at = @At(value = "FIELD",
                    target = "Lcom/elfmcys/yesstevemodel/geckolib3/model/provider/data/"
                            + "EntityModelData;netHeadYaw:F",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD),
            require = 1, expect = 1, allow = 1)
    private void nfrUi$replaceHeadYaw(@Coerce Object modelData, float originalYaw,
                                      Operation<Void> original) {
        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current();
        if (context == null || !context.active()) {
            original.call(modelData, originalYaw);
            return;
        }
        PlayerBodyRenderTransform.YsmBoneAngles angles = context.transform().ysmBoneAngles();
        original.call(modelData, (float) angles.yawDegrees);
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
}
