package neofontrender.addons.api.camera;

/** Immutable authoritative per-render camera sample. Body and view attitudes are intentionally separate. */
public final class CameraFrame {
    private final long sampleId;
    private final float partialTicks;
    private final CameraAttitude bodyAttitude;
    private final CameraAttitude viewAttitude;
    private final CameraVector bodyPosition;
    private final CameraVector position;
    private final CameraVector targetPosition;
    private final CameraVector linearVelocity;
    private final CameraVector angularVelocity;
    private final CameraBasis bodyBasis;
    private final CameraBasis viewBasis;
    private final boolean vanillaPassThrough;

    public CameraFrame(long sampleId, float partialTicks, CameraAttitude bodyAttitude,
                       CameraAttitude viewAttitude, CameraVector bodyPosition,
                       CameraVector position, boolean vanillaPassThrough) {
        this(sampleId, partialTicks, bodyAttitude, viewAttitude, bodyPosition, position,
                position, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), vanillaPassThrough);
    }

    public CameraFrame(long sampleId, float partialTicks, CameraAttitude bodyAttitude,
                       CameraAttitude viewAttitude, CameraVector bodyPosition,
                       CameraVector position, CameraVector targetPosition,
                       CameraVector linearVelocity, CameraVector angularVelocity,
                       boolean vanillaPassThrough) {
        this.sampleId = Math.max(0L, sampleId);
        this.partialTicks = Math.max(0.0F, Math.min(1.0F,
                Float.isFinite(partialTicks) ? partialTicks : 0.0F));
        this.bodyAttitude = bodyAttitude == null ? CameraAttitude.IDENTITY : bodyAttitude;
        this.viewAttitude = viewAttitude == null ? this.bodyAttitude : viewAttitude;
        this.bodyPosition = bodyPosition == null ? new CameraVector(0.0D, 0.0D, 0.0D) : bodyPosition;
        this.position = position == null ? this.bodyPosition : position;
        this.targetPosition = targetPosition == null ? this.position : targetPosition;
        this.linearVelocity = linearVelocity == null
                ? new CameraVector(0.0D, 0.0D, 0.0D) : linearVelocity;
        this.angularVelocity = angularVelocity == null
                ? new CameraVector(0.0D, 0.0D, 0.0D) : angularVelocity;
        this.bodyBasis = CameraBasis.from(this.bodyAttitude);
        this.viewBasis = CameraBasis.from(this.viewAttitude);
        this.vanillaPassThrough = vanillaPassThrough;
    }

    public long sampleId() { return sampleId; }
    public float partialTicks() { return partialTicks; }
    public CameraAttitude bodyAttitude() { return bodyAttitude; }
    public CameraAttitude viewAttitude() { return viewAttitude; }
    public CameraVector bodyPosition() { return bodyPosition; }
    public CameraVector position() { return position; }
    public CameraVector targetPosition() { return targetPosition; }
    public CameraVector linearVelocity() { return linearVelocity; }
    public CameraVector angularVelocity() { return angularVelocity; }
    public CameraBasis bodyBasis() { return bodyBasis; }
    public CameraBasis viewBasis() { return viewBasis; }
    public boolean isVanillaPassThrough() { return vanillaPassThrough; }
}
