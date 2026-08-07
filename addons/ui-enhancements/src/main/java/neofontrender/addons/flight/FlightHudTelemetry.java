package neofontrender.addons.flight;

import net.minecraft.client.entity.EntityPlayerSP;
import neofontrender.addons.api.flight.FlightTelemetry;

/** Stable render-frame telemetry sampled from the same motion fields used by vanilla Elytra travel. */
final class FlightHudTelemetry {
    private int lastTick = Integer.MIN_VALUE;
    private double lastSpeed;
    private double acceleration;

    Sample sample(EntityPlayerSP player, float partialTicks, FlightHudTheme theme,
                  float attitudePitch, float attitudeYaw) {
        double speed = Math.sqrt(player.motionX * player.motionX
                + player.motionY * player.motionY + player.motionZ * player.motionZ) * 20.0D;
        if (player.ticksExisted != lastTick) {
            if (lastTick != Integer.MIN_VALUE) {
                int elapsedTicks = Math.max(1, player.ticksExisted - lastTick);
                double measured = (speed - lastSpeed) * 20.0D / elapsedTicks;
                acceleration += (measured - acceleration) * 0.35D;
            }
            lastTick = player.ticksExisted;
            lastSpeed = speed;
        }
        double amount = Math.max(0.0D, Math.min(1.0D, partialTicks));
        double altitude = player.prevPosY + (player.posY - player.prevPosY) * amount;
        double horizontal = Math.sqrt(player.motionX * player.motionX
                + player.motionZ * player.motionZ) * 20.0D;
        double flightPathAngle = Math.toDegrees(Math.atan2(player.motionY,
                Math.max(1.0E-6D, Math.sqrt(player.motionX * player.motionX
                        + player.motionZ * player.motionZ))));
        double motionYaw = Math.toDegrees(Math.atan2(-player.motionX, player.motionZ));
        float heading = Float.isFinite(attitudeYaw) ? attitudeYaw : 0.0F;
        double drift = wrapDegrees(motionYaw - heading);
        double lowerSpeed = theme.stall.enabled ? FlightHudMath.vanillaElytraLowerSpeed(
                attitudePitch, theme.stall.referencePitch, theme.stall.margin) : 0.0D;
        return new Sample(speed, horizontal, altitude, player.motionY * 20.0D, acceleration,
                lowerSpeed, heading, flightPathAngle, drift);
    }

    private static double wrapDegrees(double value) {
        value %= 360.0D;
        if (value >= 180.0D) value -= 360.0D;
        if (value < -180.0D) value += 360.0D;
        return value;
    }

    static final class Sample {
        final double speedBlocksPerSecond;
        final double groundSpeedBlocksPerSecond;
        final double altitudeBlocks;
        final double verticalBlocksPerSecond;
        final double accelerationBlocksPerSecondSquared;
        final double lowerSpeedBlocksPerSecond;
        final float heading;
        final double flightPathAngle;
        final double driftAngle;

        private Sample(double speedBlocksPerSecond, double groundSpeedBlocksPerSecond,
                       double altitudeBlocks,
                       double verticalBlocksPerSecond, double accelerationBlocksPerSecondSquared,
                       double lowerSpeedBlocksPerSecond, float heading,
                       double flightPathAngle, double driftAngle) {
            this.speedBlocksPerSecond = speedBlocksPerSecond;
            this.groundSpeedBlocksPerSecond = groundSpeedBlocksPerSecond;
            this.altitudeBlocks = altitudeBlocks;
            this.verticalBlocksPerSecond = verticalBlocksPerSecond;
            this.accelerationBlocksPerSecondSquared = accelerationBlocksPerSecondSquared;
            this.lowerSpeedBlocksPerSecond = lowerSpeedBlocksPerSecond;
            this.heading = heading;
            this.flightPathAngle = flightPathAngle;
            this.driftAngle = driftAngle;
        }

        FlightTelemetry publicSnapshot() {
            return new FlightTelemetry(speedBlocksPerSecond, groundSpeedBlocksPerSecond,
                    altitudeBlocks, verticalBlocksPerSecond,
                    accelerationBlocksPerSecondSquared, lowerSpeedBlocksPerSecond,
                    heading, flightPathAngle, driftAngle);
        }

        static Sample from(FlightTelemetry telemetry) {
            return new Sample(telemetry.getSpeedBlocksPerSecond(),
                    telemetry.getGroundSpeedBlocksPerSecond(), telemetry.getAltitudeBlocks(),
                    telemetry.getVerticalBlocksPerSecond(),
                    telemetry.getAccelerationBlocksPerSecondSquared(),
                    telemetry.getLowerSpeedReferenceBlocksPerSecond(),
                    (float) telemetry.getHeadingDegrees(), telemetry.getFlightPathAngleDegrees(),
                    telemetry.getDriftAngleDegrees());
        }
    }
}
