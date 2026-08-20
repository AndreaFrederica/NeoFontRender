package neofontrender.addons.navigation;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiInputModality;
import neofontrender.addons.api.ui.navigation.UiInputSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiPointerStateTest {
    private static final UiInputSource CONTROLLER = new UiInputSource(
            new ResourceLocation("test", "controller"), UiInputModality.CONTROLLER);

    @Test void movedPointerDoesNotInterpolateFromThePreviousTargetEveryTick() {
        UiPointerState pointer = new UiPointerState();
        pointer.move(20.0D, 30.0D, CONTROLLER);
        pointer.move(120.0D, 80.0D, CONTROLLER);

        assertEquals(120, pointer.renderX(0.0F));
        assertEquals(80, pointer.renderY(0.0F));
        assertEquals(120, pointer.renderX(0.5F));
        assertEquals(80, pointer.renderY(0.5F));
        assertEquals(120, pointer.renderX(1.0F));
        assertEquals(80, pointer.renderY(1.0F));
        assertTrue(pointer.isSynthetic());
        assertEquals(CONTROLLER, pointer.source());
    }

    @Test void physicalMouseTakesOwnershipWithoutChangingItsCoordinates() {
        UiPointerState pointer = new UiPointerState();
        pointer.move(120.0D, 80.0D, CONTROLLER);
        pointer.physical(42.4D, 17.6D);

        assertEquals(42, pointer.renderX(0.0F));
        assertEquals(18, pointer.renderY(1.0F));
        assertFalse(pointer.isSynthetic());
        assertEquals(null, pointer.source());
    }
}
