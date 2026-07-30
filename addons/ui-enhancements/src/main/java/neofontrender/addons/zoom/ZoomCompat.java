package neofontrender.addons.zoom;

import cpw.mods.fml.common.Loader;

final class ZoomCompat {
    private ZoomCompat() {}

    static boolean optiFinePresent() {
        return Loader.isModLoaded("optifine")
                || classPresent("Config")
                // 1.7.10 OptiFine ships the launchwrapper tweaker under this name.
                || classPresent("optifine.OptiFineTweaker")
                || classPresent("optifine.OptiFineForgeTweaker");
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, ZoomCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
