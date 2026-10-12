package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraVector;

/** Pure conversion from a world-space camera origin to orientCamera's local GL translation. */
final class CameraPresentationTransform {
    private CameraPresentationTransform() {}

    static CameraVector translation(CameraAttitude view, CameraVector anchor,
                                    CameraVector camera) {
        if (view == null || anchor == null || camera == null) {
            throw new NullPointerException("view, anchor and camera are required");
        }
        CameraVector local = view.conjugate().rotate(camera.subtract(anchor));
        // UIE local +X points left, +Y points up and +Z points forward. orientCamera's
        // translation uses the same X/Z signs and the inverse Y sign.
        return new CameraVector(local.x, -local.y, local.z);
    }
}
