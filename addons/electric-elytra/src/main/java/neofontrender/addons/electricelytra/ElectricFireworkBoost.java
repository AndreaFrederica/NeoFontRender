package neofontrender.addons.electricelytra;

/** Pure additive firework acceleration in blocks/second units. */
public final class ElectricFireworkBoost {
    private ElectricFireworkBoost() {}

    public static Velocity apply(double velocityX, double velocityY, double velocityZ,
                                 double axisX, double axisY, double axisZ,
                                 double acceleration, double speedLimit) {
        double axisLength = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        if (!Double.isFinite(axisLength) || axisLength < 1.0E-9D) {
            axisX = 0.0D; axisY = 1.0D; axisZ = 0.0D;
        } else {
            axisX /= axisLength; axisY /= axisLength; axisZ /= axisLength;
        }
        double delta = Math.max(0.0D, acceleration) / 20.0D;
        velocityX += axisX * delta;
        velocityY += axisY * delta;
        velocityZ += axisZ * delta;
        double speed = Math.sqrt(velocityX * velocityX + velocityY * velocityY
                + velocityZ * velocityZ);
        if (speedLimit > 0.0D && speed > speedLimit && speed > 1.0E-9D) {
            double scale = speedLimit / speed;
            velocityX *= scale; velocityY *= scale; velocityZ *= scale;
        }
        return new Velocity(velocityX, velocityY, velocityZ);
    }

    public static final class Velocity {
        public final double x;
        public final double y;
        public final double z;

        Velocity(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }
    }
}
