package neofontrender.addons.api.camera;

/** Position of a world point relative to one immutable camera sample. */
public final class CameraRelativePose {
    private final CameraVector worldOffset;
    private final CameraVector viewOffset;
    private final double distance, bearingDegrees, elevationDegrees, angularSeparationDegrees;

    public CameraRelativePose(CameraVector worldOffset, CameraVector viewOffset, double distance,
                              double bearingDegrees, double elevationDegrees,
                              double angularSeparationDegrees) {
        this.worldOffset = worldOffset == null ? new CameraVector(0.0D, 0.0D, 0.0D) : worldOffset;
        this.viewOffset = viewOffset == null ? new CameraVector(0.0D, 0.0D, 0.0D) : viewOffset;
        this.distance = finite(distance);
        this.bearingDegrees = finite(bearingDegrees);
        this.elevationDegrees = finite(elevationDegrees);
        this.angularSeparationDegrees = finite(angularSeparationDegrees);
    }

    public CameraVector worldOffset() { return worldOffset; }
    public CameraVector viewOffset() { return viewOffset; }
    public double distance() { return distance; }
    public double bearingDegrees() { return bearingDegrees; }
    public double elevationDegrees() { return elevationDegrees; }
    public double angularSeparationDegrees() { return angularSeparationDegrees; }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
