package neofontrender.addons.electricelytra.compat;

import net.minecraft.entity.Entity;

/** Shared 1.7.10 compatibility helpers for electric elytra flight state. */
public final class ElectricElytraCompat {
    private static final int FLYING_FLAG = 1 << 7;

    private ElectricElytraCompat() {}

    public static boolean isElytraFlying(Entity entity) {
        return entity != null
                && (entity.getDataWatcher().getWatchableObjectByte(0) & FLYING_FLAG) != 0;
    }

    public static void setElytraFlying(Entity entity, boolean flying) {
        if (entity == null) return;
        byte flags = entity.getDataWatcher().getWatchableObjectByte(0);
        flags = flying ? (byte) (flags | FLYING_FLAG) : (byte) (flags & ~FLYING_FLAG);
        entity.getDataWatcher().updateObject(0, Byte.valueOf(flags));
    }
}
