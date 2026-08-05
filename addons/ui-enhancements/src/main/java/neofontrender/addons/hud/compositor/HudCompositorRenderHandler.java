package neofontrender.addons.hud.compositor;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Executes the shared HUD surface compositor exactly once at the final overlay boundary. */
public final class HudCompositorRenderHandler {
    public static final HudCompositorRenderHandler INSTANCE = new HudCompositorRenderHandler();

    private HudCompositorRenderHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            HudWindowCompositor.INSTANCE.render(event.getPartialTicks());
        }
    }
}
