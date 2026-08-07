package neofontrender.addons.api.flight;

import net.minecraft.client.entity.EntityPlayerSP;
import cpw.mods.fml.common.eventhandler.Event;

/** Forge soft-dependency hook posted after registered capability providers are evaluated. */
@Event.HasResult
public final class FlightCapabilityEvent extends Event {
    private final EntityPlayerSP player;
    private final FlightCapability capability;
    private final boolean builtInDefault;

    public FlightCapabilityEvent(EntityPlayerSP player, FlightCapability capability,
                                 boolean builtInDefault) {
        this.player = player; this.capability = capability; this.builtInDefault = builtInDefault;
    }

    public EntityPlayerSP getPlayer() { return player; }
    public FlightCapability getCapability() { return capability; }
    public boolean getBuiltInDefault() { return builtInDefault; }
}
