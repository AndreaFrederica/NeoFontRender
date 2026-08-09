package neofontrender.addons.api.flight;

/** Receives virtual-stick input. Return true to take over and prevent camera-driven control. */
@FunctionalInterface
public interface FlightManeuverHandler {
    boolean handle(FlightManeuverInput input);
}
