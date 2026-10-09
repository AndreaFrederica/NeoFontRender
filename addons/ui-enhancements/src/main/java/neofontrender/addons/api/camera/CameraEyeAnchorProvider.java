package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

/** Supplies an accepted eye point before the existing camera rigs and their collision tests. */
public interface CameraEyeAnchorProvider {
    ResourceLocation id();
    default int priority() { return 0; }

    /** Return null to defer to the next provider or to the native interpolated eye. */
    CameraEyeAnchor anchor(CameraEyeAnchorContext context);
}
