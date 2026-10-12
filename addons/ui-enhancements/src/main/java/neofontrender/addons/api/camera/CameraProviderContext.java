package neofontrender.addons.api.camera;

/** Read-only services exposed to camera providers. */
public final class CameraProviderContext {
    private final CameraFrame frame;
    private final CameraMeasurement measurement;

    public CameraProviderContext(CameraFrame frame, CameraMeasurement measurement) {
        this.frame = frame;
        this.measurement = measurement;
    }

    public CameraFrame frame() { return frame; }
    public CameraMeasurement measurement() { return measurement; }
}
