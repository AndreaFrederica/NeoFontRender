package neofontrender.addons.api.camera;

/** Atomic camera-control lease. Closing it releases only the owner's session. */
public interface CameraSession extends AutoCloseable {
    boolean isActive();
    @Override void close();
}
