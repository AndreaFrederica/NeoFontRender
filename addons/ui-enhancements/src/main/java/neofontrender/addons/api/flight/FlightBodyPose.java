package neofontrender.addons.api.flight;

import java.util.Objects;

/**
 * Complete body pose and declared local basis supplied by a flight mode.
 *
 * <p>The one-argument constructor keeps the original API 9 interpretation: the quaternion maps
 * aircraft-local +Z forward to world space. Use {@link #modelUpY(FlightAttitude)} when the
 * quaternion directly maps an upright player model's local +Y up axis to world space.</p>
 */
public final class FlightBodyPose {
    /**
     * LEGACY_AIRCRAFT_FORWARD_Z preserves the original API meaning: local +Z is the
     * aircraft's longitudinal forward axis. MODEL_UP_Y means the quaternion maps a
     * standing player-model basis, whose up axis is local +Y, directly to world space.
     */
    public enum Basis {
        LEGACY_AIRCRAFT_FORWARD_Z,
        MODEL_UP_Y
    }

    public final FlightAttitude attitude;
    public final Basis basis;
    /** Optional sampled world-space head orientation; null preserves the neutral legacy head. */
    public final FlightAttitude headWorldAttitude;

    /** Keeps the API 9 JVM descriptor and aircraft-local +Z interpretation. */
    public FlightBodyPose(FlightAttitude attitude) {
        this(attitude, Basis.LEGACY_AIRCRAFT_FORWARD_Z, null);
    }

    public FlightBodyPose(FlightAttitude attitude, Basis basis) {
        this(attitude, basis, null);
    }

    public FlightBodyPose(FlightAttitude attitude, Basis basis,
                          FlightAttitude headWorldAttitude) {
        this.attitude = Objects.requireNonNull(attitude, "attitude");
        this.basis = Objects.requireNonNull(basis, "basis");
        this.headWorldAttitude = headWorldAttitude;
    }

    /** Creates a pose whose quaternion directly maps the model's local +Y up axis to world space. */
    public static FlightBodyPose modelUpY(FlightAttitude attitude) {
        return new FlightBodyPose(attitude, Basis.MODEL_UP_Y);
    }

    /** Creates a model-up-Y pose with the head's world orientation from the same provider sample. */
    public static FlightBodyPose modelUpY(FlightAttitude bodyWorldAttitude,
                                          FlightAttitude headWorldAttitude) {
        return new FlightBodyPose(bodyWorldAttitude, Basis.MODEL_UP_Y,
                Objects.requireNonNull(headWorldAttitude, "headWorldAttitude"));
    }
}