package neofontrender.addons.api.flight;

/** Complete quaternion body pose supplied by a flight mode. */
public final class FlightBodyPose {
    public final FlightAttitude attitude;

    public FlightBodyPose(FlightAttitude attitude) {
        this.attitude = java.util.Objects.requireNonNull(attitude, "attitude");
    }
}
