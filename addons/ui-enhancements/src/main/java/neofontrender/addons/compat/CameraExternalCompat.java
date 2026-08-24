package neofontrender.addons.compat;

import net.minecraftforge.fml.common.Loader;

/** Load-time-only detection for external camera owners; never loads their classes reflectively. */
public final class CameraExternalCompat {
    private static volatile Boolean shoulderSurfing;
    private static volatile Boolean omnilook;

    private CameraExternalCompat() {}

    public static boolean shoulderSurfingPresent() {
        Boolean value = shoulderSurfing;
        if (value != null) return value;
        synchronized (CameraExternalCompat.class) {
            if (shoulderSurfing == null) shoulderSurfing = Loader.isModLoaded("shouldersurfing")
                    || resourcePresent("com/teamderpy/shouldersurfing/client/ShoulderRenderer.class");
            return shoulderSurfing;
        }
    }

    public static boolean omnilookPresent() {
        Boolean value = omnilook;
        if (value != null) return value;
        synchronized (CameraExternalCompat.class) {
            if (omnilook == null) omnilook = Loader.isModLoaded("omnilook")
                    || resourcePresent("dev/rdh/omnilook/Omnilook.class")
                    || resourcePresent("dev/rdh/omnilook/OmnilookMod.class")
                    || resourcePresent("dev/rdh/omnilook/Forgelook12.class")
                    || resourcePresent("dev/rdh/omnilook/Forgelook.class")
                    || resourcePresent("dev/rdh/omnilook/Riftlook.class");
            return omnilook;
        }
    }

    public static boolean internalFreeLookAllowed() { return !omnilookPresent(); }
    public static boolean internalShoulderAllowed() { return !shoulderSurfingPresent(); }
    /** UIE's camera owner is fully fail-closed when either legacy camera mod is installed. */
    public static boolean internalCameraAllowed() {
        return !shoulderSurfingPresent() && !omnilookPresent();
    }

    public static String failClosedReason() {
        boolean shoulder = shoulderSurfingPresent();
        boolean omni = omnilookPresent();
        if (shoulder && omni) return "external:shouldersurfing+omnilook";
        if (shoulder) return "external:shouldersurfing";
        if (omni) return "external:omnilook";
        return null;
    }

    private static boolean resourcePresent(String name) {
        ClassLoader loader = CameraExternalCompat.class.getClassLoader();
        return loader != null && loader.getResource(name) != null;
    }
}
