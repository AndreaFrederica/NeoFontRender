package neofontrender.addons.api.flight;

/** Adds normalized pitch, yaw and roll input once per captured camera frame. */
@FunctionalInterface
public interface FlightControlProvider {
    void update(FlightControlInput input);
}
