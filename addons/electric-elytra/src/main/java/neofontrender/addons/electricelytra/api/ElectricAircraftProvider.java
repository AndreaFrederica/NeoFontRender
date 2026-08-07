package neofontrender.addons.electricelytra.api;

import net.minecraft.entity.EntityLivingBase;

import javax.annotation.Nullable;

/** Return an aircraft view when this provider owns the entity, otherwise return null. */
@FunctionalInterface
public interface ElectricAircraftProvider {
    @Nullable ElectricAircraft findAircraft(EntityLivingBase entity);
}
