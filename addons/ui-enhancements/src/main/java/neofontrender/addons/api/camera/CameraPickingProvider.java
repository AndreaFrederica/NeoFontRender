package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

/** Optional picking owner. Providers are sorted by priority and may return null to delegate. */
public interface CameraPickingProvider {
    ResourceLocation id();
    default int priority() { return 0; }
    CameraHit pick(CameraPickingRequest request);
}
