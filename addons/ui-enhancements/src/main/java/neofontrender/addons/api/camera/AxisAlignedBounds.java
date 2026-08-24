package neofontrender.addons.api.camera;

/** Immutable world-space axis-aligned bounds used by camera measurements. */
public final class AxisAlignedBounds {
    private final CameraVector minimum;
    private final CameraVector maximum;

    public AxisAlignedBounds(CameraVector first, CameraVector second) {
        CameraVector a = first == null ? new CameraVector(0.0D, 0.0D, 0.0D) : first;
        CameraVector b = second == null ? a : second;
        minimum = new CameraVector(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
        maximum = new CameraVector(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    public CameraVector minimum() { return minimum; }
    public CameraVector maximum() { return maximum; }
    public CameraVector center() { return minimum.add(maximum).scale(0.5D); }
    public CameraVector size() { return maximum.subtract(minimum); }

    public CameraVector corner(int index) {
        if (index < 0 || index > 7) throw new IndexOutOfBoundsException("corner: " + index);
        return new CameraVector((index & 1) == 0 ? minimum.x : maximum.x,
                (index & 2) == 0 ? minimum.y : maximum.y,
                (index & 4) == 0 ? minimum.z : maximum.z);
    }

    public boolean contains(CameraVector point) {
        return point != null && point.x >= minimum.x && point.x <= maximum.x
                && point.y >= minimum.y && point.y <= maximum.y
                && point.z >= minimum.z && point.z <= maximum.z;
    }
}
