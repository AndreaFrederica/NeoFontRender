package neofontrender.addons.electricelytra;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.WeakHashMap;

/** Last per-entity aerodynamic acceleration vectors, consumed by the F3 renderer. */
public final class ElectricFlightDebug {
    private static final int HISTORY_LENGTH = 240;
    private static final double EPSILON = 1.0E-9D;
    private static final Map<EntityLivingBase, Sample> SAMPLES = new WeakHashMap<>();
    private static final Map<EntityLivingBase, History> HISTORIES = new WeakHashMap<>();

    private ElectricFlightDebug() {}

    static synchronized void update(EntityLivingBase entity, Vec3d bodyAxis,
                                    double velocityX, double velocityY, double velocityZ,
                                    ElectricFlightMath.FlightStep step,
                                    double thrustAcceleration) {
        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY
                + velocityZ * velocityZ);
        Vec3d velocityUnit = speed < EPSILON ? Vec3d.ZERO
                : new Vec3d(velocityX / speed, velocityY / speed, velocityZ / speed);
        Vec3d thrust = thrustAcceleration > 0.0D
                ? bodyAxis.scale(thrustAcceleration) : Vec3d.ZERO;
        Vec3d drag = velocityUnit.scale(-step.dragAcceleration);

        Vec3d lift = new Vec3d(step.liftVectorX, step.liftVectorY, step.liftVectorZ);
        Vec3d side = new Vec3d(step.sideVectorX, step.sideVectorY, step.sideVectorZ);
        ElectricBodyAxis.DynamicsSnapshot dynamics = ElectricBodyAxis.dynamics(entity);
        ItemStack chest = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        boolean sasEnabled = ItemElectricElytra.isSasEnabled(chest);
        Sample sample = new Sample(bodyAxis, thrust, lift, drag, side,
                new Vec3d(0.0D, -ElectricElytraConfig.gravityAcceleration, 0.0D),
                step.angleOfAttackRadians, step.sideslipAngleRadians,
                step.liftAcceleration, step.dragAcceleration, step.sideAcceleration,
                thrustAcceleration, dynamics.yawRateRadiansPerSecond,
                dynamics.sasYawErrorRadians,
                dynamics.sasControlAccelerationRadiansPerSecondSquared,
                dynamics.yawAccelerationRadiansPerSecondSquared, sasEnabled);
        SAMPLES.put(entity, sample);
        HISTORIES.computeIfAbsent(entity, ignored -> new History()).add(sample);
    }

    public static synchronized Sample get(EntityLivingBase entity) {
        return SAMPLES.get(entity);
    }

    public static synchronized void clear(EntityLivingBase entity) {
        SAMPLES.remove(entity);
        HISTORIES.remove(entity);
    }

    public static synchronized Sample[] history(EntityLivingBase entity) {
        History history = HISTORIES.get(entity);
        return history == null ? new Sample[0] : history.snapshot();
    }

    public static final class Sample {
        public final Vec3d bodyAxis;
        public final Vec3d thrust;
        public final Vec3d lift;
        public final Vec3d drag;
        public final Vec3d side;
        public final Vec3d gravity;
        public final double angleOfAttackRadians;
        public final double sideslipAngleRadians;
        public final double liftAcceleration;
        public final double dragAcceleration;
        public final double sideAcceleration;
        public final double thrustAcceleration;
        public final double yawRateRadiansPerSecond;
        public final double sasYawErrorRadians;
        public final double sasControlAccelerationRadiansPerSecondSquared;
        public final double yawAccelerationRadiansPerSecondSquared;
        public final boolean sasEnabled;

        Sample(Vec3d bodyAxis, Vec3d thrust, Vec3d lift, Vec3d drag,
               Vec3d side, Vec3d gravity, double angleOfAttackRadians,
               double sideslipAngleRadians, double liftAcceleration,
               double dragAcceleration, double sideAcceleration,
               double thrustAcceleration, double yawRateRadiansPerSecond,
               double sasYawErrorRadians,
               double sasControlAccelerationRadiansPerSecondSquared,
               double yawAccelerationRadiansPerSecondSquared,
               boolean sasEnabled) {
            this.bodyAxis = bodyAxis;
            this.thrust = thrust;
            this.lift = lift;
            this.drag = drag;
            this.side = side;
            this.gravity = gravity;
            this.angleOfAttackRadians = angleOfAttackRadians;
            this.sideslipAngleRadians = sideslipAngleRadians;
            this.liftAcceleration = liftAcceleration;
            this.dragAcceleration = dragAcceleration;
            this.sideAcceleration = sideAcceleration;
            this.thrustAcceleration = thrustAcceleration;
            this.yawRateRadiansPerSecond = yawRateRadiansPerSecond;
            this.sasYawErrorRadians = sasYawErrorRadians;
            this.sasControlAccelerationRadiansPerSecondSquared =
                    sasControlAccelerationRadiansPerSecondSquared;
            this.yawAccelerationRadiansPerSecondSquared =
                    yawAccelerationRadiansPerSecondSquared;
            this.sasEnabled = sasEnabled;
        }
    }

    private static final class History {
        private final Sample[] values = new Sample[HISTORY_LENGTH];
        private int next;
        private int size;

        void add(Sample sample) {
            values[next] = sample;
            next = (next + 1) % values.length;
            if (size < values.length) size++;
        }

        Sample[] snapshot() {
            Sample[] result = new Sample[size];
            int first = (next - size + values.length) % values.length;
            for (int i = 0; i < size; i++) result[i] = values[(first + i) % values.length];
            return result;
        }
    }
}
