package neofontrender.addons.electricelytra.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import neofontrender.addons.electricelytra.ElectricFlightController;
import neofontrender.addons.electricelytra.ElectricFlightPhysics;
import neofontrender.addons.electricelytra.ElectricVanillaThrust;
import neofontrender.addons.electricelytra.compat.ElectricElytraCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseElectricFlight {
    @Inject(method = "moveEntityWithHeading", at = @At("HEAD"), cancellable = true)
    private void nfr$replaceElectricElytraTravel(float strafe, float forward,
                                                  CallbackInfo callback) {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        // 1.7.10 calls this travel hook for every living entity on both sides, so the server side
        // and the locally controlled EntityPlayerSP are the only movement owners here.
        if (entity.worldObj.isRemote && !(entity instanceof EntityPlayerSP)) return;
        // Vanilla clears the Elytra flight flag before travel. Restore it from the server latch
        // or local prediction before selecting a travel solver.
        if (ElectricFlightController.isAerodynamicFlightActive(entity)
                || ElectricFlightPhysics.hasClientPrediction(entity)) {
            ElectricElytraCompat.setElytraFlying(entity, true);
        }
        // The local client predicts the aerodynamic motion. Running a second server integrator
        // would move the same player from delayed input and cause correction jitter.
        if (ElectricFlightPhysics.shouldDeferToClientMovement(entity)) {
            callback.cancel();
            return;
        }
        // This path deliberately returns to vanilla travel after adding thrust. It does not
        // invoke ElectricFlightMath, body-axis control, SAS or aerodynamic force replacement.
        if (ElectricVanillaThrust.shouldApply(entity)) ElectricVanillaThrust.apply(entity);
        if (!ElectricFlightPhysics.shouldReplaceVanillaTravel(entity)) return;

        if (entity.motionY > -0.5D) entity.fallDistance = 1.0F;
        double oldHorizontalSpeed = Math.sqrt(entity.motionX * entity.motionX
                + entity.motionZ * entity.motionZ);

        ElectricFlightPhysics.integrate(entity);
        entity.moveEntity(entity.motionX, entity.motionY, entity.motionZ);

        if (entity.isCollidedHorizontally && !entity.worldObj.isRemote) {
            double newHorizontalSpeed = Math.sqrt(entity.motionX * entity.motionX
                    + entity.motionZ * entity.motionZ);
            float damage = (float) ((oldHorizontalSpeed - newHorizontalSpeed) * 10.0D - 3.0D);
            if (damage > 0.0F) {
                entity.worldObj.playSoundAtEntity(entity, "damage.fallbig", 1.0F, 1.0F);
                entity.attackEntityFrom(DamageSource.inWall, damage);
            }
        }
        if (entity.onGround && !entity.worldObj.isRemote) {
            ElectricElytraCompat.setElytraFlying(entity, false);
        }
        callback.cancel();
    }
}
