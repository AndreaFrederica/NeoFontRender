package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;

/** Mutable per-frame contribution supplied by controller or vehicle integrations. */
public final class FlightControlInput {
    private final EntityPlayerSP player;
    private final float partialTicks;
    private final double frameSeconds;
    private float pitch;
    private float yaw;
    private float roll;

    public FlightControlInput(EntityPlayerSP player, float partialTicks, double frameSeconds) {
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
    public void setPitch(float value) { pitch = axis(value); }
    public void setYaw(float value) { yaw = axis(value); }
    public void setRoll(float value) { roll = axis(value); }
    public void addPitch(float value) { setPitch(pitch + value); }
    public void addYaw(float value) { setYaw(yaw + value); }
    public void addRoll(float value) { setRoll(roll + value); }

    private static float axis(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        return Math.max(-1.0F, Math.min(1.0F, value));
    }
}
