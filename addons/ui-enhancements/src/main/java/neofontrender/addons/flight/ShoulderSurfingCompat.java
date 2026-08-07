package neofontrender.addons.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;

/** Linkage-free Shoulder Surfing bridge limited to APIs available in 1.7.10. */
final class ShoulderSurfingCompat {
    private ShoulderSurfingCompat() {}

    static boolean isActive() {
        return ShoulderSurfingMatrixFix.isTakingOver();
    }

    /** Uses the current client ray whenever the configurable fix is enabled. */
    static Entity crosshairTarget(float partialTicks, Entity fallback) {
        if (!ShoulderSurfingFixConfig.enabled()) return fallback;
        MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
        return hit != null && hit.entityHit != null ? hit.entityHit : fallback;
    }

    /** Keeps pointedEntity aligned with the current client ray while the fix is enabled. */
    static void synchronizeMouseOver(float partialTicks) {
        if (!ShoulderSurfingFixConfig.enabled()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        MovingObjectPosition hit = minecraft.objectMouseOver;
        minecraft.pointedEntity = hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                ? hit.entityHit : null;
    }

    /** Shoulder Surfing's projected GUI offset is not exposed through 1.7.10 vanilla APIs. */
    static float[] crosshairOffset() {
        return null;
    }
}
