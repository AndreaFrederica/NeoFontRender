package neofontrender.addons.api.flight;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

/** Lifecycle events surrounding UIE's built-in schema-3 flight HUD. */
public abstract class FlightHudRenderEvent extends Event {
    private final FlightHudRenderContext context;
    protected FlightHudRenderEvent(FlightHudRenderContext context) { this.context = context; }
    public FlightHudRenderContext getContext() { return context; }

    @Cancelable
    public static final class Pre extends FlightHudRenderEvent {
        public Pre(FlightHudRenderContext context) { super(context); }
    }

    public static final class Post extends FlightHudRenderEvent {
        public Post(FlightHudRenderContext context) { super(context); }
    }
}
