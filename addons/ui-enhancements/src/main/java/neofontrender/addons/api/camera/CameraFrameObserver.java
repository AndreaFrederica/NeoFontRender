package neofontrender.addons.api.camera;

/** Read-only callback after all frame modifiers have run. */
@FunctionalInterface
public interface CameraFrameObserver {
    void onFrame(CameraFrame frame);
}
