package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

/** Optional lens stage. The highest-priority non-null lens is authoritative. */
public interface CameraLensProvider {
    ResourceLocation id();
    default int priority() { return 0; }
    CameraLens lens(CameraFrame frame, float partialTicks, CameraLens fallback);
}
