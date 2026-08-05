package neofontrender.addons.flight;

/** Runtime marker installed only by the optional Shoulder Surfing mixin. */
public final class ShoulderSurfingMatrixFix {
    private static boolean hookInstalled;

    private ShoulderSurfingMatrixFix() {}

    /** Called only from the optional Shoulder Surfing mixin. */
    public static void markHookInstalled() {
        hookInstalled = true;
    }

    public static boolean isTakingOver() {
        return hookInstalled && ShoulderSurfingFixConfig.enabled();
    }

    public static float[] crosshairOffset() {
        return ShoulderSurfingCompat.crosshairOffset();
    }

    /** Called from the optional EntityRenderer mixin after vanilla/Shoulder Surfing picking. */
    public static void synchronizeMouseOver(float partialTicks) {
        if (ShoulderSurfingFixConfig.enabled())
            ShoulderSurfingCompat.synchronizeMouseOver(partialTicks);
    }

    public static boolean dualCursorMode() {
        return ShoulderSurfingFixConfig.dual() && ShoulderSurfingCompat.isActive();
    }
}
