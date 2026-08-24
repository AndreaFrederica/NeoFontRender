package neofontrender.addons.api.camera;

/** Read-only spatial queries derived from one immutable {@link CameraFrame}. */
public final class CameraMeasurement {
    private static final int[][] BOX_EDGES = {
            {0, 1}, {2, 3}, {4, 5}, {6, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private final CameraFrame frame;
    private final int viewportWidth;
    private final int viewportHeight;
    private final double verticalFovRadians;
    private final CameraLens lens;

    public CameraMeasurement(CameraFrame frame, int viewportWidth, int viewportHeight,
                             double verticalFovDegrees) {
        this(frame, new CameraLens(viewportWidth, viewportHeight, verticalFovDegrees, 0.05D, 1024.0D));
    }

    public CameraMeasurement(CameraFrame frame, CameraLens lens) {
        this.frame = frame == null ? new CameraFrame(0L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), true) : frame;
        this.lens = lens == null ? new CameraLens(1, 1, 70.0D, 0.05D, 1024.0D) : lens;
        this.viewportWidth = this.lens.width();
        this.viewportHeight = this.lens.height();
        double degrees = this.lens.verticalFovDegrees();
        this.verticalFovRadians = Math.toRadians(Math.max(1.0D, Math.min(179.0D, degrees)));
    }

    public long sampleId() { return frame.sampleId(); }
    public CameraFrame frame() { return frame; }
    public CameraLens lens() { return lens; }
    public CameraVector worldToView(CameraVector world) {
        return world == null ? new CameraVector(0.0D, 0.0D, 0.0D)
                : frame.viewAttitude().conjugate().rotate(world.subtract(frame.position()));
    }
    public CameraVector viewToWorld(CameraVector local) {
        return local == null ? frame.position() : frame.position().add(frame.viewAttitude().rotate(local));
    }
    public CameraRay screenRay(double pixelX, double pixelY) {
        double ndcX = (2.0D * pixelX / viewportWidth) - 1.0D;
        double ndcY = 1.0D - (2.0D * pixelY / viewportHeight);
        double aspect = (double) viewportWidth / viewportHeight;
        double tangent = Math.tan(verticalFovRadians * 0.5D);
        CameraVector local = new CameraVector(-ndcX * aspect * tangent, ndcY * tangent, 1.0D);
        return new CameraRay(frame.position(), frame.viewAttitude().rotate(local));
    }

    public CameraProjection project(CameraVector world) {
        if (world == null) return new CameraProjection(CameraProjection.Visibility.INVALID, 0.0D, 0.0D, 0.0D);
        CameraVector local = worldToView(world);
        if (local.z <= 1.0E-9D) return new CameraProjection(CameraProjection.Visibility.BEHIND_CAMERA, 0.0D, 0.0D, local.z);
        double aspect = (double) viewportWidth / viewportHeight;
        double tangent = Math.tan(verticalFovRadians * 0.5D);
        double ndcX = -local.x / (local.z * aspect * tangent);
        double ndcY = local.y / (local.z * tangent);
        double pixelX = (ndcX + 1.0D) * viewportWidth * 0.5D;
        double pixelY = (1.0D - ndcY) * viewportHeight * 0.5D;
        CameraProjection.Visibility visibility;
        if (local.z < lens.nearPlane() || local.z > lens.farPlane()) {
            visibility = CameraProjection.Visibility.OUTSIDE_DEPTH_RANGE;
        } else {
            visibility = Math.abs(ndcX) <= 1.0D && Math.abs(ndcY) <= 1.0D
                    ? CameraProjection.Visibility.VISIBLE : CameraProjection.Visibility.OUTSIDE_VIEWPORT;
        }
        return new CameraProjection(visibility, pixelX, pixelY, local.z);
    }

    public double distanceTo(CameraVector world) {
        return world == null ? 0.0D : world.subtract(frame.position()).length();
    }

    /** Unit ray from the camera origin to a world-space point. */
    public CameraRay rayTo(CameraVector world) {
        return new CameraRay(frame.position(), world == null
                ? frame.viewBasis().forward() : world.subtract(frame.position()));
    }

    /** Unsigned angular separation from the center view ray, in degrees. */
    public double angularSeparationDegrees(CameraVector world) {
        if (world == null) return 0.0D;
        CameraVector direction = world.subtract(frame.position()).normalize();
        double dot = Math.max(-1.0D, Math.min(1.0D,
                direction.dot(frame.viewBasis().forward())));
        return Math.toDegrees(Math.acos(dot));
    }

    /** Horizontal bearing from screen center; positive values are to the camera's right. */
    public double bearingDegrees(CameraVector world) {
        CameraVector local = worldToView(world);
        return Math.toDegrees(Math.atan2(-local.x, local.z));
    }

    /** Vertical elevation from screen center; positive values are above the camera. */
    public double elevationDegrees(CameraVector world) {
        CameraVector local = worldToView(world);
        double horizontal = Math.sqrt(local.x * local.x + local.z * local.z);
        return Math.toDegrees(Math.atan2(local.y, horizontal));
    }

    public boolean isInsideFrustum(CameraVector world) {
        return project(world).visibility() == CameraProjection.Visibility.VISIBLE;
    }

    /** Conservative six-plane frustum test performed in quaternion-derived view space. */
    public boolean isWithinFrustum(AxisAlignedBounds bounds) {
        if (bounds == null) return false;
        CameraVector[] corners = viewCorners(bounds);
        double tangent = Math.tan(verticalFovRadians * 0.5D);
        double horizontal = tangent * lens.aspectRatio();
        if (allOutside(corners, 0, lens.nearPlane(), horizontal, tangent)) return false;
        if (allOutside(corners, 1, lens.farPlane(), horizontal, tangent)) return false;
        if (allOutside(corners, 2, 0.0D, horizontal, tangent)) return false;
        if (allOutside(corners, 3, 0.0D, horizontal, tangent)) return false;
        if (allOutside(corners, 4, 0.0D, horizontal, tangent)) return false;
        return !allOutside(corners, 5, 0.0D, horizontal, tangent);
    }

    /** Projects the visible portion of an AABB, including edges crossing near/far planes. */
    public ScreenBounds projectBounds(AxisAlignedBounds bounds) {
        if (bounds == null || !isWithinFrustum(bounds)) return ScreenBounds.invisible();
        CameraVector[] corners = viewCorners(bounds);
        java.util.List<CameraVector> candidates = new java.util.ArrayList<>();
        for (CameraVector corner : corners) {
            if (corner.z >= lens.nearPlane() && corner.z <= lens.farPlane()) candidates.add(corner);
        }
        for (int[] edge : BOX_EDGES) {
            addDepthIntersection(candidates, corners[edge[0]], corners[edge[1]], lens.nearPlane());
            addDepthIntersection(candidates, corners[edge[0]], corners[edge[1]], lens.farPlane());
        }
        if (candidates.isEmpty()) return ScreenBounds.invisible();
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (CameraVector local : candidates) {
            CameraProjection projected = projectLocal(local);
            minX = Math.min(minX, projected.pixelX());
            minY = Math.min(minY, projected.pixelY());
            maxX = Math.max(maxX, projected.pixelX());
            maxY = Math.max(maxY, projected.pixelY());
        }
        minX = Math.max(0.0D, minX);
        minY = Math.max(0.0D, minY);
        maxX = Math.min(viewportWidth, maxX);
        maxY = Math.min(viewportHeight, maxY);
        return maxX < minX || maxY < minY ? ScreenBounds.invisible()
                : new ScreenBounds(minX, minY, maxX, maxY, true);
    }

    /** Computes the visible screen segment of the world-horizontal plane. */
    public CameraHorizon horizon() {
        CameraVector localUp = frame.viewAttitude().conjugate()
                .rotate(new CameraVector(0.0D, 1.0D, 0.0D));
        double tangent = Math.tan(verticalFovRadians * 0.5D);
        double a = -localUp.x * lens.aspectRatio() * tangent;
        double b = localUp.y * tangent;
        double c = localUp.z;
        java.util.List<double[]> points = new java.util.ArrayList<>();
        addLineIntersection(points, -1.0D, true, a, b, c);
        addLineIntersection(points, 1.0D, true, a, b, c);
        addLineIntersection(points, -1.0D, false, a, b, c);
        addLineIntersection(points, 1.0D, false, a, b, c);
        double angle = Math.toDegrees(Math.atan2(-a, -b));
        if (points.size() < 2) return new CameraHorizon(false, 0.0D, 0.0D, 0.0D, 0.0D, angle);
        double[] first = points.get(0);
        double[] second = points.get(1);
        return new CameraHorizon(true, pixelX(first[0]), pixelY(first[1]),
                pixelX(second[0]), pixelY(second[1]), angle);
    }

    public CameraRelativePose relativeTo(CameraVector world) {
        CameraVector point = world == null ? frame.position() : world;
        CameraVector worldOffset = point.subtract(frame.position());
        return new CameraRelativePose(worldOffset, worldToView(point), worldOffset.length(),
                bearingDegrees(point), elevationDegrees(point), angularSeparationDegrees(point));
    }

    public CameraHit interactionTarget(CameraPickingPurpose purpose) {
        CameraPickingPurpose resolved = purpose == null ? CameraPickingPurpose.MEASUREMENT : purpose;
        double distance = resolved == CameraPickingPurpose.MEASUREMENT ? lens.farPlane() : 5.0D;
        return interactionTarget(resolved, distance, false, true);
    }

    public CameraHit interactionTarget(CameraPickingPurpose purpose, double distance,
                                       boolean includeFluids, boolean includeEntities) {
        return CameraApi.pick(new CameraPickingRequest(frame.position(), frame.viewBasis().forward(),
                distance, purpose, includeFluids, includeEntities));
    }

    private CameraVector[] viewCorners(AxisAlignedBounds bounds) {
        CameraVector[] result = new CameraVector[8];
        for (int i = 0; i < result.length; i++) result[i] = worldToView(bounds.corner(i));
        return result;
    }

    private static boolean allOutside(CameraVector[] points, int plane, double depth,
                                      double horizontal, double vertical) {
        for (CameraVector point : points) {
            boolean outside;
            switch (plane) {
                case 0: outside = point.z < depth; break;
                case 1: outside = point.z > depth; break;
                case 2: outside = point.x < -point.z * horizontal; break;
                case 3: outside = point.x > point.z * horizontal; break;
                case 4: outside = point.y < -point.z * vertical; break;
                default: outside = point.y > point.z * vertical;
            }
            if (!outside) return false;
        }
        return true;
    }

    private static void addDepthIntersection(java.util.List<CameraVector> points,
                                             CameraVector first, CameraVector second,
                                             double depth) {
        double delta = second.z - first.z;
        if (Math.abs(delta) < 1.0E-12D) return;
        double amount = (depth - first.z) / delta;
        if (amount <= 0.0D || amount >= 1.0D) return;
        points.add(new CameraVector(first.x + (second.x - first.x) * amount,
                first.y + (second.y - first.y) * amount, depth));
    }

    private CameraProjection projectLocal(CameraVector local) {
        double tangent = Math.tan(verticalFovRadians * 0.5D);
        double ndcX = -local.x / (local.z * lens.aspectRatio() * tangent);
        double ndcY = local.y / (local.z * tangent);
        return new CameraProjection(CameraProjection.Visibility.VISIBLE,
                pixelX(ndcX), pixelY(ndcY), local.z);
    }

    private static void addLineIntersection(java.util.List<double[]> points, double fixed,
                                            boolean fixedX, double a, double b, double c) {
        double denominator = fixedX ? b : a;
        if (Math.abs(denominator) < 1.0E-12D) return;
        double other = -(c + (fixedX ? a : b) * fixed) / denominator;
        if (other < -1.0D - 1.0E-9D || other > 1.0D + 1.0E-9D) return;
        double x = fixedX ? fixed : other;
        double y = fixedX ? other : fixed;
        for (double[] existing : points) {
            if (Math.abs(existing[0] - x) < 1.0E-9D && Math.abs(existing[1] - y) < 1.0E-9D) return;
        }
        points.add(new double[]{x, y});
    }

    private double pixelX(double ndcX) { return (ndcX + 1.0D) * viewportWidth * 0.5D; }
    private double pixelY(double ndcY) { return (1.0D - ndcY) * viewportHeight * 0.5D; }
}
