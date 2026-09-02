package neofontrender.addons.flight;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Handles the explicit Forge-layer suppression options used by the flight HUD. */
public final class FlightHudOverlayController {
    static final FlightHudOverlayController INSTANCE = new FlightHudOverlayController();

    private FlightHudOverlayController() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = false)
    public void beforeOverlay(RenderGameOverlayEvent.Pre event) {
        if (!FlightHudSurface.INSTANCE.visible()) return;
        if (shouldCancelForgeLayer(event.getType())) event.setCanceled(true);
    }

    static boolean shouldHideForgeCrosshairLayer() {
        return CrosshairConfig.hideForgeLayerDuringFlightHud;
    }

    static boolean shouldCancelForgeLayer(RenderGameOverlayEvent.ElementType type) {
        return type == RenderGameOverlayEvent.ElementType.CROSSHAIRS
                && shouldHideForgeCrosshairLayer();
    }

    /** Used by narrow render mixins that skip Minecraft's draw without canceling Forge events. */
    public static boolean shouldSuppressVanilla(RenderGameOverlayEvent.ElementType type) {
        return FlightHudSurface.INSTANCE.visible() && shouldHide(type);
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
            case BOSSINFO:
                return FlightRollConfig.hudHideBossBars;
            case POTION_ICONS:
                return FlightRollConfig.hudHidePotionIcons;
            case SUBTITLES:
                return FlightRollConfig.hudHideSubtitles;
            case PLAYER_LIST:
                return FlightRollConfig.hudHidePlayerList;
            case TEXT:
                return FlightRollConfig.hudHideText;
            default:
                return false;
        }
    }
}
