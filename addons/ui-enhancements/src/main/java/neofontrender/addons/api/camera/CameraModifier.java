package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

/** Immutable frame modifier, applied in descending priority after the active rig. */
public interface CameraModifier {
    ResourceLocation id();
    default int priority() { return 0; }
    CameraFrame apply(CameraFrame frame, float partialTicks);
}
