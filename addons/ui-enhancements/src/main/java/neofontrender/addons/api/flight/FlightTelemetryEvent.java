package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Mutable telemetry hook; replacement data drives both built-in and custom HUD components. */
public final class FlightTelemetryEvent extends Event {
    private final EntityPlayerSP player;
    private final float partialTicks;
    private FlightTelemetry telemetry;

    public FlightTelemetryEvent(EntityPlayerSP player, float partialTicks,
                                FlightTelemetry telemetry) {
        this.player = player; this.partialTicks = partialTicks; this.telemetry = telemetry;
    }

    public EntityPlayerSP getPlayer() { return player; }
    public float getPartialTicks() { return partialTicks; }
    public FlightTelemetry getTelemetry() { return telemetry; }
    public void setTelemetry(FlightTelemetry telemetry) {
        if (telemetry != null) this.telemetry = telemetry;
    }
}
