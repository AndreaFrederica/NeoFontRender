package neofontrender.addons.api.flight;

/** Idempotent handle returned by every dynamic flight-API registration. */
@FunctionalInterface
public interface FlightRegistration extends AutoCloseable {
    @Override void close();
}
