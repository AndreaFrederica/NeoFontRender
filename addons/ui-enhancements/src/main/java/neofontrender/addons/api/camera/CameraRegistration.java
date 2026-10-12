package neofontrender.addons.api.camera;

/** Idempotent handle returned by a camera provider or modifier registration. */
@FunctionalInterface
public interface CameraRegistration extends AutoCloseable {
    @Override void close();
}
