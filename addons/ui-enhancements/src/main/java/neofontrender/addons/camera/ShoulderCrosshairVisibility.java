package neofontrender.addons.camera;

import net.minecraft.util.math.RayTraceResult;

/** Shoulder Surfing-compatible per-perspective visibility rule. */
enum ShoulderCrosshairVisibility {
    ALWAYS,
    NEVER,
    WHEN_AIMING,
    WHEN_IN_RANGE,
    WHEN_AIMING_OR_IN_RANGE;

    boolean render(RayTraceResult hit, boolean aiming) {
        switch (this) {
            case NEVER: return false;
            case WHEN_AIMING: return aiming;
            case WHEN_IN_RANGE: return hit != null && hit.typeOfHit != RayTraceResult.Type.MISS;
            case WHEN_AIMING_OR_IN_RANGE: return aiming || (hit != null && hit.typeOfHit != RayTraceResult.Type.MISS);
            default: return true;
        }
    }
}
