package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightVector;

import java.util.Objects;

/**
 * Pure conversion from a provider's declared pose basis to the upright player-model basis.
 * The returned attitude maps model-local axes into world axes; renderer translation stays outside
 * this rotation and the YSM model's fixed 180-degree base rotation is composed separately.
 */
public final class PlayerBodyRenderTransform {
    private static final FlightAttitude LEGACY_AIRCRAFT_TO_MODEL_UP =
            FlightAttitude.axisAngle(1.0D, 0.0D, 0.0D, Math.PI / 2.0D);
    private static final FlightAttitude YSM_MODEL_BASE =
            FlightAttitude.axisAngle(0.0D, 1.0D, 0.0D, Math.PI);

    private final FlightAttitude modelToWorld;
    private final FlightAttitude headWorldAttitude;

    private PlayerBodyRenderTransform(FlightAttitude modelToWorld,
                                     FlightAttitude headWorldAttitude) {
        this.modelToWorld = Objects.requireNonNull(modelToWorld, "modelToWorld");
        this.headWorldAttitude = headWorldAttitude;
    }

    /**
     * Returns null when no body pose is available, matching {@link
     * neofontrender.addons.api.flight.FlightApi#queryBodyPose} native-fallback semantics.
     */
    public static PlayerBodyRenderTransform fromFlightPose(FlightBodyPose pose) {
        if (pose == null) return null;
        return switch (pose.basis) {
            case LEGACY_AIRCRAFT_FORWARD_Z -> new PlayerBodyRenderTransform(
                    pose.attitude.multiply(LEGACY_AIRCRAFT_TO_MODEL_UP),
                    pose.headWorldAttitude);
            case MODEL_UP_Y -> new PlayerBodyRenderTransform(
                    pose.attitude, pose.headWorldAttitude);
        };
    }

    /** Returns null when the feature is disabled or the provider has no active pose. */
    public static PlayerBodyRenderTransform resolve(FlightBodyPose pose, boolean enabled) {
        return enabled ? fromFlightPose(pose) : null;
    }

    /** The source-basis-independent quaternion that maps model-local +Y to world space. */
    public FlightAttitude modelToWorld() {
        return modelToWorld;
    }

    /**
     * YSM's fixed native player basis rotates the model by 180 degrees about +Y after the body
     * quaternion. In OpenGL order this is {@code T_render * modelToWorld * Ry(180) * S_ysm}.
     */
    public FlightAttitude ysmRootAttitude() {
        return modelToWorld.multiply(YSM_MODEL_BASE);
    }

    public FlightVector modelUpToWorld() {
        return modelToWorld.up();
    }

    /** YSM player geometry faces local -Z before its fixed 180-degree model-base rotation. */
    public FlightVector modelForwardToWorld() {
        return ysmRootAttitude().rotate(0.0D, 0.0D, -1.0D);
    }

    /**
     * Resolves YSM Head bone yaw and pitch from Minecraft angles already relative to this body.
     * YSM's EntityModelData/bone convention stores both with the opposite sign. Use
     * {@link #ysmBoneAngles(FlightAttitude)} for a sampled world-space head orientation.
     */
    public YsmBoneAngles ysmBoneAnglesFromMinecraftLocalDegrees(
            double minecraftPitchDegrees, double minecraftYawDegrees) {
        requireFinite(minecraftPitchDegrees, "minecraftPitchDegrees");
        requireFinite(minecraftYawDegrees, "minecraftYawDegrees");
        FlightAttitude minecraftLocalLook = FlightAttitude.fromMinecraftDegrees(
                minecraftPitchDegrees, minecraftYawDegrees, 0.0D);
        return ysmBoneAngles(modelToWorld.multiply(minecraftLocalLook));
    }

    /**
     * Returns the sampled head-local look in YSM bone-field signs, or neutral legacy angles when
     * the provider did not supply a head quaternion alongside its body sample.
     */
    public YsmBoneAngles ysmBoneAngles() {
        return headWorldAttitude == null
                ? YsmBoneAngles.NEUTRAL : ysmBoneAngles(headWorldAttitude);
    }

    /** Converts a world-space head orientation directly into YSM bone-field angles. */
    public YsmBoneAngles ysmBoneAngles(FlightAttitude headToWorld) {
        Objects.requireNonNull(headToWorld, "headToWorld");
        FlightVector headForwardWorld = headToWorld.forward();
        FlightVector localForward = modelToWorld.conjugate().rotate(
                headForwardWorld.x, headForwardWorld.y, headForwardWorld.z);
        double horizontal = Math.hypot(localForward.x, localForward.z);
        double yaw = Math.toDegrees(Math.atan2(localForward.x, localForward.z));
        double pitch = Math.toDegrees(Math.atan2(localForward.y, horizontal));
        return new YsmBoneAngles(yaw, pitch);
    }

    /**
     * Converts the same body-relative look to Minecraft's local head-rotation sign convention.
     * YSM's sampled head fields store both angles with the opposite sign.
     */
    public MinecraftLocalAngles minecraftLocalAngles(FlightAttitude headToWorld) {
        YsmBoneAngles ysm = ysmBoneAngles(headToWorld);
        return new MinecraftLocalAngles(-ysm.yawDegrees, -ysm.pitchDegrees);
    }

    /** Converts the sampled head orientation using Minecraft's model-part-local signs. */
    public MinecraftLocalAngles minecraftLocalAngles() {
        return headWorldAttitude == null
                ? MinecraftLocalAngles.NEUTRAL : minecraftLocalAngles(headWorldAttitude);
    }

    /** Applies the full {@code T_render * Q_root} transform to an unscaled model-local point. */
    public FlightVector modelPointInWorld(FlightVector renderOrigin, FlightVector modelPoint) {
        Objects.requireNonNull(renderOrigin, "renderOrigin");
        Objects.requireNonNull(modelPoint, "modelPoint");
        FlightVector offset = ysmRootAttitude().rotate(modelPoint.x, modelPoint.y, modelPoint.z);
        return new FlightVector(renderOrigin.x + offset.x,
                renderOrigin.y + offset.y, renderOrigin.z + offset.z);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }

    /** Angles expressed in the exact YSM bone fields after conversion through the body frame. */
    public static final class YsmBoneAngles {
        private static final YsmBoneAngles NEUTRAL = new YsmBoneAngles(0.0D, 0.0D);

        public final double yawDegrees;
        public final double pitchDegrees;

        private YsmBoneAngles(double yawDegrees, double pitchDegrees) {
            this.yawDegrees = yawDegrees;
            this.pitchDegrees = pitchDegrees;
        }
    }

    /** Angles expressed in Minecraft model-part local head rotation convention. */
    public static final class MinecraftLocalAngles {
        private static final MinecraftLocalAngles NEUTRAL =
                new MinecraftLocalAngles(0.0D, 0.0D);

        public final double yawDegrees;
        public final double pitchDegrees;

        private MinecraftLocalAngles(double yawDegrees, double pitchDegrees) {
            this.yawDegrees = yawDegrees;
            this.pitchDegrees = pitchDegrees;
        }
    }
}
