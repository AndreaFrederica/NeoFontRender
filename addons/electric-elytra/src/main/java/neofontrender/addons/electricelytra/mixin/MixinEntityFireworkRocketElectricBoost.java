package neofontrender.addons.electricelytra.mixin;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import neofontrender.addons.electricelytra.ElectricFireworkBoostState;
import neofontrender.addons.electricelytra.ItemElectricElytra;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses vanilla's velocity steering and delegates sustained boost to electric physics. */
@Mixin(EntityFireworkRocket.class)
public abstract class MixinEntityFireworkRocketElectricBoost {
    @Shadow private EntityLivingBase boostedEntity;
    @Unique private boolean nfrElectric$replaceBoost;
    @Unique private double nfrElectric$motionX;
    @Unique private double nfrElectric$motionY;
    @Unique private double nfrElectric$motionZ;
    @Unique private EntityLivingBase nfrElectric$target;

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void nfrElectric$captureVelocity(CallbackInfo ci) {
        nfrElectric$replaceBoost = isElectricFlight(boostedEntity);
        if (!nfrElectric$replaceBoost) return;
        nfrElectric$target = boostedEntity;
        nfrElectric$motionX = nfrElectric$target.motionX;
        nfrElectric$motionY = nfrElectric$target.motionY;
        nfrElectric$motionZ = nfrElectric$target.motionZ;
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void nfrElectric$applyAdditiveBoost(CallbackInfo ci) {
        if (!nfrElectric$replaceBoost || nfrElectric$target == null) return;
        // Vanilla may steer the player toward its 1.5-vector target inside onUpdate. Restore the
        // exact pre-call velocity, then let ElectricFlightPhysics apply one continuous force.
        nfrElectric$target.motionX = nfrElectric$motionX;
        nfrElectric$target.motionY = nfrElectric$motionY;
        nfrElectric$target.motionZ = nfrElectric$motionZ;
        if (isElectricFlight(nfrElectric$target)) {
            ElectricFireworkBoostState.markActive(nfrElectric$target);
        }
        nfrElectric$target = null;
    }

    @Unique
    private static boolean isElectricFlight(EntityLivingBase entity) {
        if (entity == null || !entity.isElytraFlying()) return false;
        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        return ItemElectricElytra.usesAerodynamicFlightModel(chest);
    }
}
