package neofontrender.addons.api.camera;

/** Cause of a camera ownership or perspective transition. */
public enum CameraViewChangeReason {
    MODE_ENTER,
    MODE_EXIT,
    PERSPECTIVE_CHANGED,
    WORLD_CHANGED
}
