package neofontrender.addons.api.flight;

/** One render-frame sample shared by camera, HUD and addon instruments. */
public final class FlightRenderPose {
    private final FlightAttitude attitude;
    private final FlightEulerAngles cameraAngles;
    private final float partialTicks;

    public FlightRenderPose(FlightAttitude attitude, FlightEulerAngles cameraAngles,
                            float partialTicks) {
        this.attitude = java.util.Objects.requireNonNull(attitude, "attitude");
        this.cameraAngles = java.util.Objects.requireNonNull(cameraAngles, "cameraAngles");
        this.partialTicks = Math.max(0.0F, Math.min(1.0F, partialTicks));
    }

    public FlightAttitude getAttitude() { return attitude; }
    public FlightEulerAngles getCameraAngles() { return cameraAngles; }
    public float getPartialTicks() { return partialTicks; }
}
