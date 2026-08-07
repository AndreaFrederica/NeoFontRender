package neofontrender.addons.electricelytra.api;

/** Close the handle when a dynamically installed provider is no longer valid. */
@FunctionalInterface
public interface ElectricAircraftRegistration extends AutoCloseable {
    @Override void close();
}
