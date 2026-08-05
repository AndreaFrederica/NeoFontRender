package neofontrender.addons.flight;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightHudOverlayControllerTest {
    @Test
    void mapsGroupedAndIndependentVanillaHudElements() {
        boolean oldStatus = FlightRollConfig.hudHidePlayerStatus;
        boolean oldBoss = FlightRollConfig.hudHideBossBars;
        boolean oldChat = FlightRollConfig.hudHideChat;
        boolean oldForgeCrosshair = CrosshairConfig.hideForgeLayerDuringFlightHud;
        try {
            FlightRollConfig.hudHidePlayerStatus = true;
            FlightRollConfig.hudHideBossBars = true;
            FlightRollConfig.hudHideChat = false;

            assertTrue(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.HEALTH));
            assertTrue(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.ARMOR));
            assertTrue(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.JUMPBAR));
            assertTrue(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.BOSSHEALTH));
            assertTrue(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.BOSSINFO));
            assertFalse(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.CHAT));
            assertFalse(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.CROSSHAIRS));
            assertFalse(FlightHudOverlayController.shouldHide(
                    RenderGameOverlayEvent.ElementType.ALL));
            CrosshairConfig.hideForgeLayerDuringFlightHud = false;
            assertFalse(FlightHudOverlayController.shouldHideForgeCrosshairLayer());
            CrosshairConfig.hideForgeLayerDuringFlightHud = true;
            assertTrue(FlightHudOverlayController.shouldHideForgeCrosshairLayer());
        } finally {
            FlightRollConfig.hudHidePlayerStatus = oldStatus;
            FlightRollConfig.hudHideBossBars = oldBoss;
            FlightRollConfig.hudHideChat = oldChat;
            CrosshairConfig.hideForgeLayerDuringFlightHud = oldForgeCrosshair;
        }
    }
}
