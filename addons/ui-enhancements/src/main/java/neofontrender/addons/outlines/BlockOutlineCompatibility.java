package neofontrender.addons.outlines;

import neofontrender.addons.compat.ClassPresenceChecker;

final class BlockOutlineCompatibility {
    private static final boolean ACTINIUM_PRESENT =
            ClassPresenceChecker.isPresent("com/gtnewhorizons/angelica/glsm/GLStateManager.class");
    private BlockOutlineCompatibility() {}
    static boolean actiniumPresent() { return ACTINIUM_PRESENT; }
    static String effectiveMode(String configured) {
        return ACTINIUM_PRESENT ? BlockOutlineConfig.MODE_NATIVE : configured;
    }
}
