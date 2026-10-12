package neofontrender.addons.camera;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import neofontrender.addons.api.camera.CameraVector;

/**
 * 1.7.10 has no Valkyrien Skies build, so there is no ship transform or ship-aware ray tracer to
 * hook. The adapter keeps the 1.12.2 call shape as an explicit degradation: mounted offsets are
 * returned unchanged and no ship is ever excluded from picking. Every caller therefore behaves
 * exactly like vanilla on this platform, and {@link #isAvailable()} reports the inactive state.
 */
final class ValkyrienCameraCompat {
    private ValkyrienCameraCompat() {}

    static CameraVector rotateMountedOffset(Entity entity, CameraVector offset) {
        return offset;
    }

    static Object excludePilotedShip(World world, Entity player) {
        return null;
    }

    static void restorePilotedShip(World world, Object ship) {
    }

    static boolean isAvailable() {
        return false;
    }
}
