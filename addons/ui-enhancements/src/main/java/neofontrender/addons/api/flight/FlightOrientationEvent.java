package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Mutable, cancelable local-axis orientation update posted immediately before application. */
@Cancelable
public final class FlightOrientationEvent extends Event {
    private final EntityPlayerSP player;
    private double pitch, yaw, roll;

    public FlightOrientationEvent(EntityPlayerSP player, double pitch, double yaw, double roll) {
        this.player = player; this.pitch = finite(pitch); this.yaw = finite(yaw); this.roll = finite(roll);
    }

    public EntityPlayerSP getPlayer() { return player; }
    public double getPitchDegrees() { return pitch; }
    public double getYawDegrees() { return yaw; }
    public double getRollDegrees() { return roll; }
    public void setPitchDegrees(double value) { pitch = finite(value); }
    public void setYawDegrees(double value) { yaw = finite(value); }
    public void setRollDegrees(double value) { roll = finite(value); }

    private static double finite(double value) { return Double.isFinite(value) ? value : 0.0D; }
}
