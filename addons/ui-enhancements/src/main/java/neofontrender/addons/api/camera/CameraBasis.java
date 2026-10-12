package neofontrender.addons.api.camera;

/** Orthonormal basis derived from one authoritative camera quaternion. */
public final class CameraBasis {
    private final CameraVector forward;
    private final CameraVector right;
    private final CameraVector up;

    private CameraBasis(CameraVector forward, CameraVector right, CameraVector up) {
        this.forward = forward; this.right = right; this.up = up;
    }

    public static CameraBasis from(CameraAttitude attitude) {
        return new CameraBasis(attitude.forward(), attitude.right(), attitude.up());
    }
    public CameraVector forward() { return forward; }
    public CameraVector right() { return right; }
    public CameraVector up() { return up; }

    /** Column-major rotation matrix mapping world vectors into OpenGL camera space. */
    public float[] openGlViewMatrix() {
        CameraVector backward = forward.scale(-1.0D);
        return new float[]{
                (float) right.x, (float) up.x, (float) backward.x, 0.0F,
                (float) right.y, (float) up.y, (float) backward.y, 0.0F,
                (float) right.z, (float) up.z, (float) backward.z, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        };
    }
}
