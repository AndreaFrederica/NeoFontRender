package neofontrender.addons.camera;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraCollisionQuery;

/** Position rig derived from Shoulder Surfing 2.9.6 (MIT), rewritten around UIE quaternions. */
final class ShoulderCameraRig {
    private static CameraVector previousTransition = new CameraVector(ShoulderCameraConfig.offsetX,
            ShoulderCameraConfig.offsetY, -ShoulderCameraConfig.offsetZ);
    private static CameraVector currentTransition = previousTransition;

    private ShoulderCameraRig() {}

    static void reset(EntityPlayerSP player, CameraAttitude view, CameraVector anchor) {
        CameraVector target = targetOffset(player, view, anchor);
        previousTransition = target;
        currentTransition = target;
    }

    /** Advances only at the simulation boundary; render samples interpolate this state. */
    static void tick(EntityPlayerSP player, CameraAttitude view, CameraVector anchor) {
        CameraVector target = targetOffset(player, view, anchor);
        previousTransition = currentTransition;
        double response = transitionBlend(ShoulderCameraConfig.transitionSpeed, 1.0D / 20.0D);
        currentTransition = interpolate(currentTransition, target, response);
    }

    static Sample resolve(EntityPlayerSP player, CameraAttitude view, CameraVector anchor,
                          float partialTicks) {
        CameraVector worldOffset = view.rotate(interpolate(
                previousTransition, currentTransition, clampPartial(partialTicks)));
        worldOffset = ValkyrienCameraCompat.rotateMountedOffset(player, worldOffset);
        CameraVector desired = anchor.add(worldOffset);
        if (!ShoulderCameraConfig.collision || player.world == null) {
            CameraVector external = CameraApi.resolveCollision(
                    new CameraCollisionQuery(anchor, desired, 0.1D));
            return new Sample(external == null ? desired : external, desired);
        }
        Vec3d origin = new Vec3d(anchor.x, anchor.y, anchor.z);
        Vec3d direction = new Vec3d(desired.x - anchor.x, desired.y - anchor.y,
                desired.z - anchor.z);
        double length = Math.sqrt(direction.x * direction.x + direction.y * direction.y
                + direction.z * direction.z);
        if (length < 1.0E-8D) return new Sample(anchor, anchor);
        double allowed = length;
        Object excludedShip = ValkyrienCameraCompat.excludePilotedShip(player.world, player);
        try {
            for (int i = 0; i < 8; i++) {
                CameraVector localCorner = new CameraVector((i & 1) == 0 ? -0.1D : 0.1D,
                        (i & 2) == 0 ? -0.1D : 0.1D,
                        (i & 4) == 0 ? -0.1D : 0.1D);
                CameraVector worldCorner = view.rotate(localCorner);
                Vec3d from = origin.add(worldCorner.x, worldCorner.y, worldCorner.z);
                Vec3d to = from.add(direction);
                RayTraceResult hit = player.world.rayTraceBlocks(from, to, false, true, false);
                if (hit != null && hit.hitVec != null) {
                    allowed = Math.min(allowed,
                            Math.max(0.0D, origin.distanceTo(hit.hitVec)));
                }
            }
        } finally {
            ValkyrienCameraCompat.restorePilotedShip(player.world, excludedShip);
        }
        CameraVector resolved = allowed >= length ? desired : new CameraVector(
                origin.x + direction.x * allowed / length,
                origin.y + direction.y * allowed / length,
                origin.z + direction.z * allowed / length);
        CameraVector external = CameraApi.resolveCollision(new CameraCollisionQuery(anchor, resolved, 0.1D));
        return new Sample(external == null ? resolved : external, desired);
    }

    static CameraVector desired(CameraVector anchor, CameraAttitude view) {
        return anchor.add(view.rotate(new CameraVector(-0.75D, 0.0D, -3.0D)));
    }

    private static CameraVector desired(CameraVector anchor, CameraAttitude view, CameraVector local) {
        return anchor.add(view.rotate(local));
    }

    private static CameraVector targetOffset(EntityPlayerSP player, CameraAttitude view,
                                             CameraVector anchor) {
        double x = ShoulderCameraConfig.offsetX;
        double y = ShoulderCameraConfig.offsetY;
        double z = -ShoulderCameraConfig.offsetZ;
        if (player.isRiding()) {
            x *= ShoulderCameraConfig.passengerXMultiplier;
            y *= ShoulderCameraConfig.passengerYMultiplier;
            z *= ShoulderCameraConfig.passengerZMultiplier;
        }
        if (player.isSprinting()) {
            x *= ShoulderCameraConfig.sprintXMultiplier;
            y *= ShoulderCameraConfig.sprintYMultiplier;
            z *= ShoulderCameraConfig.sprintZMultiplier;
        }
        if (ShoulderCameraConfig.centerWhenClimbing && player.isOnLadder()) x = 0.0D;
        double threshold = ShoulderCameraConfig.centerWhenLookingDownDegrees;
        if (threshold > 0.0D && view.forward().y <= -Math.cos(Math.toRadians(threshold))) {
            x = 0.0D;
            y = 0.0D;
        }
        CameraVector target = new CameraVector(x, y, z);
        if (ShoulderCameraConfig.dynamicallyAdjustOffsets && player.world != null) {
            target = adaptToSpace(player, view, anchor, target);
        }
        return target;
    }

    static double transitionBlend(double originalTickResponse, double seconds) {
        double response = Math.max(0.0D, Math.min(1.0D, originalTickResponse));
        if (response >= 1.0D) return 1.0D;
        double rate = -Math.log(1.0D - response) * 20.0D;
        return 1.0D - Math.exp(-rate * Math.max(0.0D, seconds));
    }

    static CameraVector interpolate(CameraVector previous, CameraVector current, double amount) {
        double value = Math.max(0.0D, Math.min(1.0D, amount));
        return new CameraVector(previous.x + (current.x - previous.x) * value,
                previous.y + (current.y - previous.y) * value,
                previous.z + (current.z - previous.z) * value);
    }

    private static double clampPartial(float value) {
        return Float.isFinite(value) ? Math.max(0.0D, Math.min(1.0D, value)) : 0.0D;
    }

    private static CameraVector adaptToSpace(EntityPlayerSP player, CameraAttitude view,
                                              CameraVector anchor, CameraVector local) {
        double depth = Math.abs(local.z);
        if (depth < 1.0E-8D) return local;
        CameraVector worldOffset = view.rotate(local);
        CameraVector forward = view.forward();
        CameraVector lateral = worldOffset.subtract(forward.scale(worldOffset.dot(forward)));
        Vec3d eye = new Vec3d(anchor.x, anchor.y, anchor.z);
        double targetX = Math.abs(local.x);
        double targetY = Math.abs(local.y);
        double clearance = player.width / 3.0D;
        for (double dz = 0.0D; dz <= depth; dz += 0.03125D) {
            double scale = dz / depth;
            CameraVector along = worldOffset.scale(scale);
            Vec3d from = eye.add(along.x, along.y, along.z);
            CameraVector toward = lateral.add(forward.scale(-dz));
            Vec3d to = eye.add(toward.x, toward.y, toward.z);
            RayTraceResult hit = player.world.rayTraceBlocks(from, to, false, true, false);
            if (hit == null || hit.hitVec == null) continue;
            double distance = hit.hitVec.distanceTo(from);
            targetX = Math.min(targetX, Math.max(distance + Math.abs(local.x) * scale - clearance, 0.0D));
            targetY = Math.min(targetY, Math.max(distance + Math.abs(local.y) * scale - clearance, 0.0D));
        }
        return new CameraVector(Math.copySign(targetX, local.x), Math.copySign(targetY, local.y), local.z);
    }

    static final class Sample {
        final CameraVector position;
        final CameraVector target;

        Sample(CameraVector position, CameraVector target) {
            this.position = position;
            this.target = target;
        }
    }
}
