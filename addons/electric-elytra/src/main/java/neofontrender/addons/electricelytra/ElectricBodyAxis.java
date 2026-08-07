package neofontrender.addons.electricelytra;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightVector;

import java.util.Map;
import java.util.WeakHashMap;

/** Quaternion aircraft attitude and body-local maneuver dynamics. */
public final class ElectricBodyAxis {
    private static final double EPSILON = 1.0E-9D;
    private static final Map<EntityLivingBase, State> STATES = new WeakHashMap<>();

    private ElectricBodyAxis() {}

    public static synchronized FlightAttitude sampleAttitude(EntityLivingBase entity,
                                                              float partialTicks) {
        State state = STATES.computeIfAbsent(entity, State::new);
        advance(entity, state);
        return state.previous.slerp(state.current,
                Math.max(0.0D, Math.min(1.0D, partialTicks)));
    }

    public static synchronized FlightVector sampleForward(EntityLivingBase entity,
                                                           float partialTicks) {
        return sampleAttitude(entity, partialTicks).forward();
    }

    public static synchronized void setManeuverCommand(EntityLivingBase entity,
                                                       double pitch, double roll,
                                                       double rudder) {
        State state = STATES.computeIfAbsent(entity, State::new);
        state.pitchCommand = axis(pitch);
        state.rollCommand = axis(roll);
        state.rudderCommand = axis(rudder);
    }

    public static synchronized void reset(EntityLivingBase entity) { STATES.remove(entity); }

    public static synchronized DynamicsSnapshot dynamics(EntityLivingBase entity) {
        State state = STATES.get(entity);
        return state == null ? DynamicsSnapshot.ZERO : new DynamicsSnapshot(
                state.sideslip, state.yawRate, state.sasYawError,
                state.sasControlAcceleration, state.yawAcceleration);
    }

    public static synchronized double sampleRudderCommand(EntityLivingBase entity) {
        State state = STATES.get(entity);
        return state == null ? 0.0D : state.rudderCommand;
    }

    private static void advance(EntityLivingBase entity, State state) {
        int elapsed = Math.max(0, Math.min(20, entity.ticksExisted - state.tick));
        if (elapsed == 0) return;
        double maximumPitch = Math.toRadians(
                ElectricElytraConfig.bodyAxisTurnRateDegreesPerSecond) / 20.0D;
        double maximumRoll = Math.toRadians(
                ElectricElytraConfig.bodyRollRateDegreesPerSecond) / 20.0D;
        for (int tick = 0; tick < elapsed; tick++) {
            state.previous = state.current;
            ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            boolean sas = ItemElectricElytra.isSasEnabled(chest);
            FlightAttitude sasTarget = sas ? ItemElectricElytra.getSasTarget(chest) : null;
            double deadzone = ElectricElytraConfig.sasManualInputDeadzone;
            boolean manualPitch = Math.abs(state.pitchCommand) > deadzone;
            boolean manualYaw = Math.abs(state.rudderCommand) > deadzone;
            boolean manualRoll = Math.abs(state.rollCommand) > deadzone;
            boolean manual = manualPitch || manualYaw || manualRoll;

            if (sas && sasTarget != null && !manual) {
                double error = state.current.angularDistance(sasTarget);
                double step = Math.min(maximumPitch,
                        error * ElectricElytraConfig.sasAttitudeResponsePerSecond / 20.0D);
                state.current = rotateTowards(state.current, sasTarget, step);
                state.sasControlAcceleration = ElectricElytraConfig.sasAttitudeResponsePerSecond
                        * ElectricElytraConfig.sasAttitudeResponsePerSecond * error;
            } else {
                state.current = state.current.rotateLocal(1.0D, 0.0D, 0.0D,
                        state.pitchCommand * maximumPitch);
                state.current = state.current.rotateLocal(0.0D, 0.0D, 1.0D,
                        state.rollCommand * maximumRoll);
                state.sasControlAcceleration = 0.0D;
            }
            applyAerodynamicYaw(entity, state, sas && !manual, sasTarget);
            if (sas && manual) ItemElectricElytra.setSas(chest, true, state.current);
        }
        state.tick = entity.ticksExisted;
    }

    private static void applyAerodynamicYaw(EntityLivingBase entity, State state,
                                            boolean sas, FlightAttitude sasTarget) {
        double velocityX = entity.motionX * 20.0D;
        double velocityY = entity.motionY * 20.0D;
        double velocityZ = entity.motionZ * 20.0D;
        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY
                + velocityZ * velocityZ);
        FlightVector forward = state.current.forward();
        FlightVector right = state.current.right();
        double sideslip = 0.0D;
        if (speed > ElectricElytraConfig.minimumAerodynamicSpeed) {
            double localForward = (velocityX * forward.x + velocityY * forward.y
                    + velocityZ * forward.z) / speed;
            double localRight = (velocityX * right.x + velocityY * right.y
                    + velocityZ * right.z) / speed;
            sideslip = ElectricFlightMath.sideslipAngle(localForward, localRight);
        }
        double sasYawError = 0.0D;
        if (sas && sasTarget != null) {
            sasYawError = signedAngleAroundAxis(forward, sasTarget.forward(),
                    state.current.up().scale(-1.0D));
        }
        double yawAcceleration = ElectricFlightMath.yawAngularAcceleration(speed, sideslip,
                state.rudderCommand, state.yawRate, sas, sasYawError);
        state.sideslip = sideslip;
        state.sasYawError = sasYawError;
        state.yawAcceleration = yawAcceleration;
        state.yawRate += yawAcceleration / 20.0D;
        double limit = Math.toRadians(ElectricElytraConfig.bodyAxisTurnRateDegreesPerSecond);
        state.yawRate = Math.max(-limit, Math.min(limit, state.yawRate));
        // Positive rudder/yaw turns around aircraft-local down.
        state.current = state.current.rotateLocal(0.0D, -1.0D, 0.0D,
                state.yawRate / 20.0D);
    }

    private static double signedAngleAroundAxis(FlightVector from, FlightVector to,
                                                FlightVector axis) {
        FlightVector projectedFrom = new FlightVector(from.x - axis.x * from.dot(axis),
                from.y - axis.y * from.dot(axis), from.z - axis.z * from.dot(axis));
        FlightVector projectedTo = new FlightVector(to.x - axis.x * to.dot(axis),
                to.y - axis.y * to.dot(axis), to.z - axis.z * to.dot(axis));
        if (projectedFrom.lengthSquared() < EPSILON || projectedTo.lengthSquared() < EPSILON)
            return 0.0D;
        projectedFrom = projectedFrom.normalize(); projectedTo = projectedTo.normalize();
        return Math.atan2(axis.dot(projectedFrom.cross(projectedTo)),
                Math.max(-1.0D, Math.min(1.0D, projectedFrom.dot(projectedTo))));
    }

    private static FlightAttitude rotateTowards(FlightAttitude current, FlightAttitude target,
                                                 double maximumAngle) {
        double angle = current.angularDistance(target);
        return angle <= maximumAngle || angle < EPSILON ? target
                : current.slerp(target, maximumAngle / angle);
    }

    private static double axis(double value) {
        return Math.max(-1.0D, Math.min(1.0D, Double.isFinite(value) ? value : 0.0D));
    }

    private static final class State {
        FlightAttitude previous;
        FlightAttitude current;
        int tick;
        double pitchCommand, rollCommand, rudderCommand;
        double yawRate, sideslip, sasYawError, sasControlAcceleration, yawAcceleration;

        State(EntityLivingBase entity) {
            current = FlightAttitude.fromMinecraftDegrees(
                    entity.rotationPitch, entity.rotationYaw, 0.0D);
            previous = current;
            tick = entity.ticksExisted - 1;
        }
    }

    public static final class DynamicsSnapshot {
        static final DynamicsSnapshot ZERO = new DynamicsSnapshot(0.0D, 0.0D,
                0.0D, 0.0D, 0.0D);
        public final double sideslipRadians;
        public final double yawRateRadiansPerSecond;
        public final double sasYawErrorRadians;
        public final double sasControlAccelerationRadiansPerSecondSquared;
        public final double yawAccelerationRadiansPerSecondSquared;
        DynamicsSnapshot(double sideslip, double yawRate, double sasError,
                         double sasControl, double yawAcceleration) {
            this.sideslipRadians = sideslip; this.yawRateRadiansPerSecond = yawRate;
            this.sasYawErrorRadians = sasError;
            this.sasControlAccelerationRadiansPerSecondSquared = sasControl;
            this.yawAccelerationRadiansPerSecondSquared = yawAcceleration;
        }
    }
}
