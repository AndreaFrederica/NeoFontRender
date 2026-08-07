package neofontrender.addons.api.flight.server;

/** Idempotent server-policy registration handle. */
@FunctionalInterface
public interface FlightServerRegistration extends AutoCloseable {
    @Override void close();
}
