package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

/** Optional collision stage for blocks, ships, portals, or mod-owned geometry. */
public interface CameraCollisionProvider {
    ResourceLocation id();
    default int priority() { return 0; }
    CameraVector resolve(CameraCollisionQuery query);
}
