package neofontrender.addons.camera;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraCollisionQuery;
import neofontrender.addons.api.camera.CameraVector;

/** Quaternion-native equivalent of vanilla's third-person orbit and eight-ray collision test. */
final class FreeLookCameraRig {
    private static CameraVector previousMoveOffset = new CameraVector(0.0D, 0.0D, 0.0D);
    private static CameraVector currentMoveOffset = previousMoveOffset;
    private static boolean moveTickInitialized;

    private FreeLookCameraRig() {}

    static void adjustMoveOffset(double dx, double dy, double dz) {
        if (!moveTickInitialized) {
            previousMoveOffset = currentMoveOffset;
            moveTickInitialized = true;
        }
        currentMoveOffset = currentMoveOffset.add(new CameraVector(dx, dy, dz));
    }

    static void resetMoveOffset() {
        if (!moveTickInitialized) {
            previousMoveOffset = currentMoveOffset;
            moveTickInitialized = true;
        }
        currentMoveOffset = new CameraVector(0.0D, 0.0D, 0.0D);
    }

    /** Captures the previous tick before the client mutates the current camera offset. */
    static void beginTick() {
        previousMoveOffset = currentMoveOffset;
        moveTickInitialized = true;
    }

    static double moveOffsetX() { return currentMoveOffset.x; }
    static double moveOffsetY() { return currentMoveOffset.y; }
    static double moveOffsetZ() { return currentMoveOffset.z; }

    static CameraVector interpolatedMoveOffset(float partialTicks) {
        double amount = Float.isFinite(partialTicks)
                ? Math.max(0.0D, Math.min(1.0D, partialTicks)) : 0.0D;
        return new CameraVector(
                previousMoveOffset.x + (currentMoveOffset.x - previousMoveOffset.x) * amount,
                previousMoveOffset.y + (currentMoveOffset.y - previousMoveOffset.y) * amount,
                previousMoveOffset.z + (currentMoveOffset.z - previousMoveOffset.z) * amount);
    }

    static Sample resolve(EntityPlayerSP player, CameraAttitude view, CameraVector anchor,
                          float partialTicks) {
        return resolve(player, view, anchor, FreeLookConfig.distance,
                FreeLookConfig.collision, true, interpolatedMoveOffset(partialTicks));
    }

    static Sample resolveVanilla(EntityPlayerSP player, CameraAttitude view, CameraVector anchor,
                                 double distance) {
        return resolve(player, view, anchor, distance, true, false);
    }

    private static Sample resolve(EntityPlayerSP player, CameraAttitude view, CameraVector anchor,
                                  double distance, boolean collision, boolean externalCollision) {
        return resolve(player, view, anchor, distance, collision, externalCollision,
                new CameraVector(0.0D, 0.0D, 0.0D));
    }

    private static Sample resolve(EntityPlayerSP player, CameraAttitude view, CameraVector anchor,
                                  double distance, boolean collision, boolean externalCollision,
                                  CameraVector localMove) {
        CameraVector offset = view.forward().scale(-distance);
        offset = ValkyrienCameraCompat.rotateMountedOffset(player, offset);
        // Apply user-controlled position offset in camera-local space
        CameraVector worldMove = view.rotate(localMove);
        CameraVector displacement = offset.add(worldMove);
        CameraVector target = anchor.add(displacement);
        CameraVector resolved = target;
        if (collision && player.world != null) {
            double length = displacement.length();
            double allowed = length;
            Vec3d origin = vec(anchor);
            for (int i = 0; i < 8; i++) {
                CameraVector corner = new CameraVector((i & 1) == 0 ? -0.1D : 0.1D,
                        (i & 2) == 0 ? -0.1D : 0.1D,
                        (i & 4) == 0 ? -0.1D : 0.1D);
                Vec3d from = origin.add(corner.x, corner.y, corner.z);
                RayTraceResult hit = player.world.rayTraceBlocks(from,
                        from.add(displacement.x, displacement.y, displacement.z),
                        false, true, false);
                if (hit != null && hit.hitVec != null) {
                    allowed = Math.min(allowed, Math.max(0.0D, origin.distanceTo(hit.hitVec)));
                }
            }
            if (length > 1.0E-8D && allowed < length) {
                resolved = anchor.add(displacement.scale(allowed / length));
            }
        }
        if (externalCollision) {
            CameraVector external = CameraApi.resolveCollision(
                    new CameraCollisionQuery(anchor, resolved, 0.1D));
            if (external != null) resolved = external;
        }
        return new Sample(resolved, target);
    }

    private static Vec3d vec(CameraVector value) { return new Vec3d(value.x, value.y, value.z); }

    static final class Sample {
        final CameraVector position;
        final CameraVector target;

        Sample(CameraVector position, CameraVector target) {
            this.position = position;
            this.target = target;
        }
    }
}
