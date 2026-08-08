package neofontrender.addons.electricelytra.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.entity.MoverType;
import neofontrender.addons.electricelytra.ElectricFlightPhysics;
import neofontrender.addons.electricelytra.ElectricVanillaThrust;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseElectricFlight {
    @Shadow protected abstract SoundEvent getFallSound(int damageValue);

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void nfr$replaceElectricElytraTravel(float strafe, float vertical, float forward,
                                                  CallbackInfo callback) {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        if (!(entity.isServerWorld() || entity.canPassengerSteer())) return;
        // This path deliberately returns to vanilla travel after adding thrust. It does not
        // invoke ElectricFlightMath, body-axis control, SAS or aerodynamic force replacement.
        if (ElectricVanillaThrust.shouldApply(entity)) ElectricVanillaThrust.apply(entity);
        if (!ElectricFlightPhysics.shouldReplaceVanillaTravel(entity)) return;

        if (entity.motionY > -0.5D) entity.fallDistance = 1.0F;
        double oldHorizontalSpeed = Math.sqrt(entity.motionX * entity.motionX
                + entity.motionZ * entity.motionZ);

        ElectricFlightPhysics.integrate(entity);
        entity.move(MoverType.SELF, entity.motionX, entity.motionY, entity.motionZ);

        if (entity.collidedHorizontally && !entity.world.isRemote) {
            double newHorizontalSpeed = Math.sqrt(entity.motionX * entity.motionX
                    + entity.motionZ * entity.motionZ);
            float damage = (float) ((oldHorizontalSpeed - newHorizontalSpeed) * 10.0D - 3.0D);
            if (damage > 0.0F) {
                entity.playSound(getFallSound((int) damage), 1.0F, 1.0F);
                entity.attackEntityFrom(DamageSource.FLY_INTO_WALL, damage);
            }
        }
        if (entity.onGround && !entity.world.isRemote) entity.setFlag(7, false);
        callback.cancel();
    }
}
