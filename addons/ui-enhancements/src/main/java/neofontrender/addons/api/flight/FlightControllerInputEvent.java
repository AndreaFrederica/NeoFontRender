package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Frame-rate controller input hook for UIE's three-axis flight mode.
 *
 * <p>Controller integrations may set normalized pitch, yaw and roll axes in the range
 * {@code [-1, 1]}. UIE applies its controller sensitivity and inversion settings afterwards.
 * The event is posted on the Forge event bus once per captured camera-input frame while the
 * local player is flying with UIE flight control enabled.</p>
 */
public final class FlightControllerInputEvent extends Event {
    private final EntityPlayerSP player;
    private final float partialTicks;
    private final double frameSeconds;
    private float pitch;
    private float yaw;
    private float roll;

    public FlightControllerInputEvent(EntityPlayerSP player, float partialTicks, double frameSeconds) {
        this.player = player;
        this.partialTicks = partialTicks;
        this.frameSeconds = frameSeconds;
    }

    public EntityPlayerSP getPlayer() { return player; }
    public float getPartialTicks() { return partialTicks; }
    public double getFrameSeconds() { return frameSeconds; }
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }
    public float getRoll() { return roll; }
    public void setPitch(float pitch) { this.pitch = axis(pitch); }
    public void setYaw(float yaw) { this.yaw = axis(yaw); }
    public void setRoll(float roll) { this.roll = axis(roll); }

    private static float axis(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        return Math.max(-1.0F, Math.min(1.0F, value));
    }
}
