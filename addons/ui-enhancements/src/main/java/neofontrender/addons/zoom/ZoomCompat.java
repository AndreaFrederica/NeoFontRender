package neofontrender.addons.zoom;

import net.minecraftforge.fml.common.Loader;

final class ZoomCompat {
    private ZoomCompat() {}

    static boolean optiFinePresent() {
        return Loader.isModLoaded("optifine")
                || classPresent("Config")
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
