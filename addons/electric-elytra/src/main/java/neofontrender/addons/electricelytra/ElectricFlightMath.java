package neofontrender.addons.electricelytra;

import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightVector;

/**
 * Point-mass aerodynamic integrator. Public inputs and outputs use blocks/second;
 * Minecraft entity motion is converted at the integration boundary.
 */
public final class ElectricFlightMath {
    private static final double TICK_SECONDS = 1.0D / 20.0D;
    private static final double EPSILON = 1.0E-9D;

    private ElectricFlightMath() {}

    public static FlightStep step(double velocityX, double velocityY, double velocityZ,
                                  double bodyAxisX, double bodyAxisY, double bodyAxisZ,
                                  boolean engineEnabled, double throttle) {
        return step(velocityX, velocityY, velocityZ, bodyAxisX, bodyAxisY, bodyAxisZ,
                engineEnabled, throttle, 0);
    }

    public static FlightStep step(double velocityX, double velocityY, double velocityZ,
                                  double bodyAxisX, double bodyAxisY, double bodyAxisZ,
                                  boolean engineEnabled, double throttle, int flapSetting) {
        return step(velocityX, velocityY, velocityZ, bodyAxisX, bodyAxisY, bodyAxisZ,
                0.0D, engineEnabled, throttle, flapSetting);
    }

    public static FlightStep step(double velocityX, double velocityY, double velocityZ,
                                  double bodyAxisX, double bodyAxisY, double bodyAxisZ,
                                  double rollRadians, boolean engineEnabled,
                                  double throttle, int flapSetting) {
        return step(velocityX, velocityY, velocityZ, bodyAxisX, bodyAxisY, bodyAxisZ,
                rollRadians, engineEnabled, throttle, flapSetting, 0.0D);
    }

    public static FlightStep step(double velocityX, double velocityY, double velocityZ,
                                  double bodyAxisX, double bodyAxisY, double bodyAxisZ,
                                  double rollRadians, boolean engineEnabled,
                                  double throttle, int flapSetting, double rudderInput) {
        return step(velocityX, velocityY, velocityZ, bodyAxisX, bodyAxisY, bodyAxisZ,
                rollRadians, engineEnabled, throttle, flapSetting, rudderInput, 0.0D);
    }

    public static FlightStep step(double velocityX, double velocityY, double velocityZ,
                                  double bodyAxisX, double bodyAxisY, double bodyAxisZ,
                                  double rollRadians, boolean engineEnabled,
                                  double throttle, int flapSetting, double rudderInput,
                                  double additionalThrustAcceleration) {
        return step(velocityX, velocityY, velocityZ,
                legacyAttitude(bodyAxisX, bodyAxisY, bodyAxisZ, rollRadians),
                engineEnabled, throttle, flapSetting, rudderInput,
                additionalThrustAcceleration);
    }

    public static FlightStep step(double velocityX, double velocityY, double velocityZ,
                                  FlightAttitude attitude, boolean engineEnabled,
                                  double throttle, int flapSetting, double rudderInput,
                                  double additionalThrustAcceleration) {
        throttle = clamp(throttle, 0.0D, 1.0D);
        flapSetting = Math.max(0, Math.min(2, flapSetting));
        rudderInput = clamp(rudderInput, -1.0D, 1.0D);
        additionalThrustAcceleration = Math.max(0.0D,
                Double.isFinite(additionalThrustAcceleration)
                        ? additionalThrustAcceleration : 0.0D);

        double speed = length(velocityX, velocityY, velocityZ);
        if (attitude == null) attitude = FlightAttitude.IDENTITY;
        FlightVector forward = attitude.forward();
        FlightVector rolledRight = attitude.right();
        FlightVector rolledUp = attitude.up();
        double bodyAxisX = forward.x, bodyAxisY = forward.y, bodyAxisZ = forward.z;
        double rolledRightX = rolledRight.x, rolledRightY = rolledRight.y,
                rolledRightZ = rolledRight.z;
        double rolledUpX = rolledUp.x, rolledUpY = rolledUp.y, rolledUpZ = rolledUp.z;

        double thrustAcceleration = additionalThrustAcceleration
                + (engineEnabled ? ElectricElytraConfig.maximumThrustAcceleration * throttle : 0.0D);
        double accelerationX = bodyAxisX * thrustAcceleration;
        double accelerationY = -ElectricElytraConfig.gravityAcceleration;
        double accelerationZ = bodyAxisZ * thrustAcceleration;
        accelerationY += bodyAxisY * thrustAcceleration;

        double angleOfAttack = 0.0D;
        double liftCoefficient = 0.0D;
        double dragCoefficient = 0.0D;
        double liftAcceleration = 0.0D;
        double dragAcceleration = 0.0D;
        double sideslipAngle = 0.0D;
        double sideAcceleration = 0.0D;
        double liftVectorX = 0.0D;
        double liftVectorY = 0.0D;
        double liftVectorZ = 0.0D;
        double sideVectorX = 0.0D;
        double sideVectorY = 0.0D;
        double sideVectorZ = 0.0D;

        if (speed > ElectricElytraConfig.minimumAerodynamicSpeed) {
            double velocityUnitX = velocityX / speed;
            double velocityUnitY = velocityY / speed;
            double velocityUnitZ = velocityZ / speed;

            double localForwardSpeed = velocityUnitX * bodyAxisX
                    + velocityUnitY * bodyAxisY + velocityUnitZ * bodyAxisZ;
            double localUpSpeed = velocityUnitX * rolledUpX
                    + velocityUnitY * rolledUpY + velocityUnitZ * rolledUpZ;
            angleOfAttack = Math.atan2(-localUpSpeed, localForwardSpeed);

            double flapLift = flapSetting == 1 ? ElectricElytraConfig.takeoffFlapLiftIncrement
                    : flapSetting == 2 ? ElectricElytraConfig.landingFlapLiftIncrement : 0.0D;
            double flapMaximumLift = flapSetting == 1
                    ? ElectricElytraConfig.takeoffFlapMaximumLiftIncrement
                    : flapSetting == 2
                    ? ElectricElytraConfig.landingFlapMaximumLiftIncrement : 0.0D;
            double flapDrag = flapSetting == 1 ? ElectricElytraConfig.takeoffFlapDragIncrement
                    : flapSetting == 2 ? ElectricElytraConfig.landingFlapDragIncrement : 0.0D;
            double linearLift = ElectricElytraConfig.liftCoefficientAtZeroAngle + flapLift
                    + ElectricElytraConfig.liftCurveSlope * angleOfAttack;
            liftCoefficient = clamp(linearLift,
                    -ElectricElytraConfig.maximumLiftCoefficient,
                    ElectricElytraConfig.maximumLiftCoefficient + flapMaximumLift);

            double stallAngle = Math.toRadians(ElectricElytraConfig.stallAngleDegrees);
            double stallProgress = clamp((Math.abs(angleOfAttack) - stallAngle)
                    / Math.max(EPSILON, Math.PI / 2.0D - stallAngle), 0.0D, 1.0D);
            double separatedFlow = smoothStep(stallProgress);
            liftCoefficient *= 1.0D - 0.72D * separatedFlow;

            double inducedDrag = liftCoefficient * liftCoefficient
                    / (Math.PI * ElectricElytraConfig.wingAspectRatio
                    * ElectricElytraConfig.oswaldEfficiency);
            dragCoefficient = ElectricElytraConfig.zeroLiftDragCoefficient + flapDrag + inducedDrag
                    + ElectricElytraConfig.stallDragCoefficient * separatedFlow * separatedFlow;

            double dynamicPressure = 0.5D * ElectricElytraConfig.airDensity
                    * speed * speed;
            double forceToAcceleration = ElectricElytraConfig.wingArea
                    / ElectricElytraConfig.totalMass;
            liftAcceleration = dynamicPressure * forceToAcceleration * liftCoefficient;
            dragAcceleration = dynamicPressure * forceToAcceleration * dragCoefficient;

            accelerationX -= velocityUnitX * dragAcceleration;
            accelerationY -= velocityUnitY * dragAcceleration;
            accelerationZ -= velocityUnitZ * dragAcceleration;

            double localRightSpeed = velocityUnitX * rolledRightX
                    + velocityUnitY * rolledRightY + velocityUnitZ * rolledRightZ;
            sideslipAngle = sideslipAngle(localForwardSpeed, localRightSpeed);
            double sideCoefficient = sideForceCoefficient(sideslipAngle, rudderInput);
            sideAcceleration = dynamicPressure * forceToAcceleration * sideCoefficient;
            sideVectorX = rolledRightX * sideAcceleration;
            sideVectorY = rolledRightY * sideAcceleration;
            sideVectorZ = rolledRightZ * sideAcceleration;
            accelerationX += sideVectorX;
            accelerationY += sideVectorY;
            accelerationZ += sideVectorZ;

            // Project the rolled wing normal onto the plane normal to relative airflow.
            double upAlongVelocity = rolledUpX * velocityUnitX
                    + rolledUpY * velocityUnitY + rolledUpZ * velocityUnitZ;
            double liftX = rolledUpX - velocityUnitX * upAlongVelocity;
            double liftY = rolledUpY - velocityUnitY * upAlongVelocity;
            double liftZ = rolledUpZ - velocityUnitZ * upAlongVelocity;
            double liftLength = length(liftX, liftY, liftZ);
            if (liftLength > EPSILON) {
                liftVectorX = liftX / liftLength * liftAcceleration;
                liftVectorY = liftY / liftLength * liftAcceleration;
                liftVectorZ = liftZ / liftLength * liftAcceleration;
                accelerationX += liftVectorX;
                accelerationY += liftVectorY;
                accelerationZ += liftVectorZ;
            }
        }

        velocityX += accelerationX * TICK_SECONDS;
        velocityY += accelerationY * TICK_SECONDS;
        velocityZ += accelerationZ * TICK_SECONDS;

        double updatedSpeed = length(velocityX, velocityY, velocityZ);
        double hardLimit = ElectricElytraConfig.hardSpeedLimitBlocksPerSecond;
        if (updatedSpeed > hardLimit && updatedSpeed > EPSILON) {
            double scale = hardLimit / updatedSpeed;
            velocityX *= scale;
            velocityY *= scale;
            velocityZ *= scale;
        }

        return new FlightStep(velocityX, velocityY, velocityZ, angleOfAttack,
                liftCoefficient, dragCoefficient, liftAcceleration, dragAcceleration,
                sideslipAngle, sideAcceleration, liftVectorX, liftVectorY, liftVectorZ,
                sideVectorX, sideVectorY, sideVectorZ);
    }

    private static FlightAttitude legacyAttitude(double forwardX, double forwardY,
                                                 double forwardZ, double rollRadians) {
        FlightVector forward = new FlightVector(forwardX, forwardY, forwardZ).normalize();
        if (forward.lengthSquared() < EPSILON) forward = new FlightVector(0.0D, 0.0D, 1.0D);
        FlightVector reference = Math.abs(forward.y) > 0.999D
                ? new FlightVector(0.0D, 0.0D, 1.0D) : new FlightVector(0.0D, 1.0D, 0.0D);
        FlightVector right = forward.cross(reference).normalize();
        FlightVector up = right.cross(forward).normalize();
        return FlightAttitude.fromBasis(right, up, forward)
                .rotateLocal(0.0D, 0.0D, 1.0D, rollRadians);
    }

    static int indicatedPower(int throttlePercent) {
        return Math.max(0, Math.min(100, throttlePercent));
    }

    static double directionalYawMomentCoefficient(double sideslipRadians,
                                                  double rudderInput) {
        // Positive beta means airflow/velocity is toward aircraft-right. Positive yaw also
        // turns right, so the weathercock moment must be positive to reduce beta.
        return ElectricElytraConfig.yawMomentCoefficientPerRadian
                * saturatedSideslip(sideslipRadians)
                + ElectricElytraConfig.rudderYawMomentCoefficient
                * clamp(rudderInput, -1.0D, 1.0D);
    }

    static double directionalYawMomentCoefficient(double sideslipRadians,
                                                   double rudderInput,
                                                   double yawRateRadiansPerSecond,
                                                   double speedBlocksPerSecond) {
        double speed = Math.max(EPSILON, Math.abs(speedBlocksPerSecond));
        double normalizedYawRate = yawRateRadiansPerSecond
                * ElectricElytraConfig.yawReferenceLength / (2.0D * speed);
        return directionalYawMomentCoefficient(sideslipRadians, rudderInput)
                - ElectricElytraConfig.yawRateMomentCoefficient * normalizedYawRate;
    }

    static double sideslipAngle(double localForwardSpeed, double localRightSpeed) {
        if (!Double.isFinite(localForwardSpeed) || !Double.isFinite(localRightSpeed)) return 0.0D;
        return Math.atan2(localRightSpeed, localForwardSpeed);
    }

    static double sideForceCoefficient(double sideslipRadians, double rudderInput) {
        // Lateral force separates at large beta. sin(beta) preserves the small-angle
        // derivative while preventing the old unbounded linear side-force spike.
        return -ElectricElytraConfig.sideForceCoefficientPerRadian
                * Math.sin(wrapRadians(sideslipRadians))
                + ElectricElytraConfig.rudderSideForceCoefficient
                * clamp(rudderInput, -1.0D, 1.0D);
    }

    static double yawAngularAcceleration(double speedBlocksPerSecond,
                                         double sideslipRadians, double rudderInput,
                                         double yawRateRadiansPerSecond,
                                         boolean sas, double sasYawErrorRadians) {
        double speed = Math.max(0.0D, speedBlocksPerSecond);
        double acceleration = 0.0D;
        if (speed > ElectricElytraConfig.minimumAerodynamicSpeed) {
            double dynamicPressure = 0.5D * ElectricElytraConfig.airDensity * speed * speed;
            acceleration = dynamicPressure * ElectricElytraConfig.wingArea
                    * ElectricElytraConfig.yawReferenceLength
                    * directionalYawMomentCoefficient(sideslipRadians, rudderInput,
                    yawRateRadiansPerSecond, speed)
                    / ElectricElytraConfig.yawInertia;
        }
        if (sas) {
            double response = ElectricElytraConfig.sasAttitudeResponsePerSecond;
            acceleration += response * response * wrapRadians(sasYawErrorRadians);
        }
        double damping = sas ? ElectricElytraConfig.sasYawRateDamping
                : ElectricElytraConfig.yawRateDamping;
        acceleration -= damping * yawRateRadiansPerSecond;
        double limit = Math.toRadians(
                ElectricElytraConfig.maximumYawAccelerationDegreesPerSecondSquared);
        return clamp(acceleration, -limit, limit);
    }

    private static double saturatedSideslip(double sideslipRadians) {
        double wrapped = wrapRadians(sideslipRadians);
        double saturation = Math.toRadians(ElectricElytraConfig.sideslipSaturationDegrees);
        return saturation * Math.tanh(wrapped / Math.max(EPSILON, saturation));
    }

    public static double stallSpeedBlocksPerSecond(int flapSetting) {
        flapSetting = Math.max(0, Math.min(2, flapSetting));
        double flapMaximumLift = flapSetting == 1
                ? ElectricElytraConfig.takeoffFlapMaximumLiftIncrement
                : flapSetting == 2
                ? ElectricElytraConfig.landingFlapMaximumLiftIncrement : 0.0D;
        double maximumLift = Math.max(EPSILON,
                ElectricElytraConfig.maximumLiftCoefficient + flapMaximumLift);
        return Math.sqrt(2.0D * ElectricElytraConfig.totalMass
                * ElectricElytraConfig.gravityAcceleration
                / (ElectricElytraConfig.airDensity * ElectricElytraConfig.wingArea
                * maximumLift));
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double smoothStep(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double wrapRadians(double angle) {
        while (angle > Math.PI) angle -= Math.PI * 2.0D;
        while (angle < -Math.PI) angle += Math.PI * 2.0D;
        return angle;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class FlightStep {
        public final double velocityX;
        public final double velocityY;
        public final double velocityZ;
        public final double angleOfAttackRadians;
        public final double liftCoefficient;
        public final double dragCoefficient;
        public final double liftAcceleration;
        public final double dragAcceleration;
        public final double sideslipAngleRadians;
        public final double sideAcceleration;
        public final double liftVectorX;
        public final double liftVectorY;
        public final double liftVectorZ;
        public final double sideVectorX;
        public final double sideVectorY;
        public final double sideVectorZ;

        FlightStep(double velocityX, double velocityY, double velocityZ,
                   double angleOfAttackRadians, double liftCoefficient,
                   double dragCoefficient, double liftAcceleration, double dragAcceleration,
                   double sideslipAngleRadians, double sideAcceleration,
                   double liftVectorX, double liftVectorY, double liftVectorZ,
                   double sideVectorX, double sideVectorY, double sideVectorZ) {
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.angleOfAttackRadians = angleOfAttackRadians;
            this.liftCoefficient = liftCoefficient;
            this.dragCoefficient = dragCoefficient;
            this.liftAcceleration = liftAcceleration;
            this.dragAcceleration = dragAcceleration;
            this.sideslipAngleRadians = sideslipAngleRadians;
            this.sideAcceleration = sideAcceleration;
            this.liftVectorX = liftVectorX;
            this.liftVectorY = liftVectorY;
            this.liftVectorZ = liftVectorZ;
            this.sideVectorX = sideVectorX;
            this.sideVectorY = sideVectorY;
            this.sideVectorZ = sideVectorZ;
        }
    }
}
