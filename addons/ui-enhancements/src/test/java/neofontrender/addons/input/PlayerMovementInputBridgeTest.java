package neofontrender.addons.input;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputContext;
import neofontrender.addons.api.input.InputFrame;
import neofontrender.addons.api.input.InputFrameContext;
import neofontrender.addons.api.input.InputRegistration;
import neofontrender.addons.api.input.InputValue;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMovementInputBridgeTest {
    @Test
    void preservesSignedAnalogMovementAndButtonActions() {
        Map<InputAction, InputValue> values = new EnumMap<>(InputAction.class);
        values.put(InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(0.72F));
        values.put(InputAction.PLAYER_MOVE_STRAFE, InputValue.axis(-0.35F));
        values.put(InputAction.PLAYER_JUMP, InputValue.button(true, true, false));
        InputFrame frame = InputApi.publish(context(1L), values);

        PlayerMovementInputBridge.State state = PlayerMovementInputBridge.resolve(
                frame, 0.0F, 0.0F, false, false, false);

        assertEquals(0.72F, state.forward, 1.0E-6F);
        assertEquals(-0.35F, state.strafe, 1.0E-6F);
        assertTrue(state.jump);
        assertFalse(state.sneak);
    }

    @Test
    void sneakScalesBothAnalogAxesLikeForge112MovementInput() {
        Map<InputAction, InputValue> values = new EnumMap<>(InputAction.class);
        values.put(InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(-0.8F));
        values.put(InputAction.PLAYER_MOVE_STRAFE, InputValue.axis(0.5F));
        values.put(InputAction.PLAYER_SNEAK, InputValue.button(true, true, false));
        InputFrame frame = InputApi.publish(context(2L), values);

        PlayerMovementInputBridge.State state = PlayerMovementInputBridge.resolve(
                frame, 0.0F, 0.0F, false, false, false);

        assertEquals(-0.24F, state.forward, 1.0E-6F);
        assertEquals(0.15F, state.strafe, 1.0E-6F);
        assertTrue(state.sneak);
    }

    @Test
    void modalContextBlocksMovementAndFreeLookFlightSuppressesStrafe() {
        InputRegistration block = InputApi.pushContext(InputContext.builder(
                        new ResourceLocation("test", "modal"), 100)
                .block(InputAction.PLAYER_MOVE_FORWARD, InputAction.PLAYER_JUMP)
                .build());
        try {
            Map<InputAction, InputValue> values = new EnumMap<>(InputAction.class);
            values.put(InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(1.0F));
            values.put(InputAction.PLAYER_MOVE_STRAFE, InputValue.axis(-0.75F));
            values.put(InputAction.PLAYER_JUMP, InputValue.button(true, true, false));
            InputFrame frame = InputApi.publish(context(3L), values);

            PlayerMovementInputBridge.State state = PlayerMovementInputBridge.resolve(
                    frame, 1.0F, 1.0F, true, false, true);

            assertEquals(0.0F, state.forward, 1.0E-6F);
            assertEquals(0.0F, state.strafe, 1.0E-6F);
            assertFalse(state.jump);
        } finally {
            block.close();
        }
    }

    @Test
    void cameraRelativeForwardIsRotatedIntoBodySpace() {
        float[] axes = PlayerMovementInputBridge.rotateAxes(1.0F, 0.0F, 90.0F);

        assertEquals(0.0F, axes[0], 1.0E-6F);
        assertEquals(-1.0F, axes[1], 1.0E-6F);
    }

    @Test
    void cameraRelativeRotationPreservesAnalogMagnitudeAndDiagonals() {
        float[] axes = PlayerMovementInputBridge.rotateAxes(0.8F, -0.35F, -37.0F);

        assertEquals(Math.sqrt(0.8F * 0.8F + 0.35F * 0.35F),
                Math.sqrt(axes[0] * axes[0] + axes[1] * axes[1]), 1.0E-6D);
    }

    private static InputFrameContext context(long sampleId) {
        return new InputFrameContext(sampleId, 0.0F, 1.0D / 60.0D, true);
    }
}
