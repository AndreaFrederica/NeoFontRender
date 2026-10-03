package neofontrender.addons.flight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrosshairControllerRoutingTest {
    @Test
    void itemModCrosshairGetsShoulderOffsetWhenCustomRendererIsDisabled() {
        assertTrue(ModCrosshairRouting.shouldOffset(false, false));
        assertTrue(ModCrosshairRouting.shouldOffset(false, true));
    }

    @Test
    void customRendererOffsetsOnlyThePreferredModPath() {
        assertFalse(ModCrosshairRouting.shouldOffset(true, false));
        assertTrue(ModCrosshairRouting.shouldOffset(true, true));
    }

    @Test
    void flightHudRestoresOnlyAVisiblePlayerAimCrosshair() {
        assertTrue(ModCrosshairRouting.shouldRenderFlightAim(true, true, true));
        assertFalse(ModCrosshairRouting.shouldRenderFlightAim(false, true, true));
        assertFalse(ModCrosshairRouting.shouldRenderFlightAim(true, false, true));
        assertFalse(ModCrosshairRouting.shouldRenderFlightAim(true, true, false));
    }

    @Test
    void crosshairEventCancellationIsIndependentForGroundAndFlight() {
        boolean originalGround = CrosshairConfig.cancelCrosshairEventOnGround;
        boolean originalFlight = CrosshairConfig.cancelCrosshairEventDuringFlight;
        try {
            CrosshairConfig.cancelCrosshairEventOnGround = true;
            CrosshairConfig.cancelCrosshairEventDuringFlight = false;
            assertTrue(CrosshairEventPolicy.shouldCancel(false));
            assertFalse(CrosshairEventPolicy.shouldCancel(true));

            CrosshairConfig.cancelCrosshairEventOnGround = false;
            CrosshairConfig.cancelCrosshairEventDuringFlight = true;
            assertFalse(CrosshairEventPolicy.shouldCancel(false));
            assertTrue(CrosshairEventPolicy.shouldCancel(true));
        } finally {
            CrosshairConfig.cancelCrosshairEventOnGround = originalGround;
            CrosshairConfig.cancelCrosshairEventDuringFlight = originalFlight;
        }
    }
}
