package neofontrender.addons.electricelytra;

import net.minecraft.entity.EntityLivingBase;

import java.util.Map;
import java.util.WeakHashMap;

/** Bridges a burning vanilla rocket into the single electric-flight physics integrator. */
public final class ElectricFireworkBoostState {
    private static final Map<EntityLivingBase, Long> LAST_ACTIVE_TICK = new WeakHashMap<>();

    private ElectricFireworkBoostState() {}

    public static synchronized void markActive(EntityLivingBase entity) {
        if (entity != null && entity.worldObj != null) {
            LAST_ACTIVE_TICK.put(entity, entity.worldObj.getTotalWorldTime());
        }
    }

    public static synchronized boolean isActive(EntityLivingBase entity) {
        if (entity == null || entity.worldObj == null) return false;
        Long tick = LAST_ACTIVE_TICK.get(entity);
        return tick != null && entity.worldObj.getTotalWorldTime() - tick <= 1L;
    }

    public static synchronized void clear(EntityLivingBase entity) {
        LAST_ACTIVE_TICK.remove(entity);
    }
}
