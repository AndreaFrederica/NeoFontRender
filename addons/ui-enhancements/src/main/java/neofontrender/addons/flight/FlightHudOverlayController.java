package neofontrender.addons.flight;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Selectively suppresses Forge's vanilla HUD elements while the flight HUD is active. */
public final class FlightHudOverlayController {
    static final FlightHudOverlayController INSTANCE = new FlightHudOverlayController();

    private FlightHudOverlayController() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public void beforeOverlay(RenderGameOverlayEvent.Pre event) {
        if (!FlightHudSurface.INSTANCE.visible()) return;
        if (event.type == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            if (shouldHideForgeCrosshairLayer()) event.setCanceled(true);
            return;
        }
        if (shouldHide(event.type)) event.setCanceled(true);
    }

    static boolean shouldHideForgeCrosshairLayer() {
        return CrosshairConfig.hideForgeLayerDuringFlightHud;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public void beforeHand(RenderHandEvent event) {
        if (FlightHudSurface.INSTANCE.visible() && FlightRollConfig.hudHideFirstPersonHand) {
            event.setCanceled(true);
        }
    }

    static boolean shouldHide(RenderGameOverlayEvent.ElementType type) {
        switch (type) {
            case HOTBAR:
                return FlightRollConfig.hudHideHotbar;
            case ARMOR:
            case HEALTH:
            case FOOD:
            case AIR:
            case HEALTHMOUNT:
            case JUMPBAR:
                return FlightRollConfig.hudHidePlayerStatus;
            case EXPERIENCE:
                return FlightRollConfig.hudHideExperience;
            case CHAT:
                return FlightRollConfig.hudHideChat;
            case BOSSHEALTH:
                return FlightRollConfig.hudHideBossBars;
            case PLAYER_LIST:
                return FlightRollConfig.hudHidePlayerList;
            case TEXT:
                return FlightRollConfig.hudHideText;
            default:
                return false;
        }
    }
}
