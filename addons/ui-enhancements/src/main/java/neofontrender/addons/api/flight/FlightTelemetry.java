package neofontrender.addons.api.flight;

/** Immutable SI/Minecraft-unit telemetry. Presentation-unit conversion belongs to renderers. */
public final class FlightTelemetry {
    private final double speed;
    private final double groundSpeed;
    private final double altitude;
    private final double verticalSpeed;
    private final double acceleration;
    private final double lowerSpeedReference;
    private final double heading;
    private final double flightPathAngle;
    private final double driftAngle;

    public FlightTelemetry(double speed, double groundSpeed, double altitude,
                           double verticalSpeed, double acceleration,
                           double lowerSpeedReference, double heading,
                           double flightPathAngle, double driftAngle) {
        this.speed = finite(speed); this.groundSpeed = finite(groundSpeed);
        this.altitude = finite(altitude); this.verticalSpeed = finite(verticalSpeed);
        this.acceleration = finite(acceleration); this.lowerSpeedReference = finite(lowerSpeedReference);
        this.heading = finite(heading); this.flightPathAngle = finite(flightPathAngle);
        this.driftAngle = finite(driftAngle);
    }

    public double getSpeedBlocksPerSecond() { return speed; }
    public double getGroundSpeedBlocksPerSecond() { return groundSpeed; }
    public double getAltitudeBlocks() { return altitude; }
    public double getVerticalBlocksPerSecond() { return verticalSpeed; }
    public double getAccelerationBlocksPerSecondSquared() { return acceleration; }
    public double getLowerSpeedReferenceBlocksPerSecond() { return lowerSpeedReference; }
    public double getHeadingDegrees() { return heading; }
    public double getFlightPathAngleDegrees() { return flightPathAngle; }
    public double getDriftAngleDegrees() { return driftAngle; }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
