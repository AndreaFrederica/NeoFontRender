package neofontrender.addons.camera;

import net.minecraft.util.math.MathHelper;

/** Resolves cursor-owned head and body yaw without feeding either back into the camera. */
final class CursorAimPose {
    static final float HEAD_YAW_LIMIT = 75.0F;

    final float bodyYaw;
    final float headYaw;
    final float pitch;

    private CursorAimPose(float bodyYaw, float headYaw, float pitch) {
        this.bodyYaw = bodyYaw;
        this.headYaw = headYaw;
        this.pitch = pitch;
    }

    static CursorAimPose resolve(float currentBodyYaw, float targetYaw, float pitch,
                                 boolean headOnly) {
        float delta = MathHelper.wrapDegrees(targetYaw - currentBodyYaw);
        float headYaw = currentBodyYaw + delta;
        if (!headOnly) return new CursorAimPose(headYaw, headYaw, pitch);

        float bodyYaw = currentBodyYaw;
        if (delta > HEAD_YAW_LIMIT) bodyYaw += delta - HEAD_YAW_LIMIT;
        else if (delta < -HEAD_YAW_LIMIT) bodyYaw += delta + HEAD_YAW_LIMIT;
        return new CursorAimPose(bodyYaw, headYaw, pitch);
    }
}
