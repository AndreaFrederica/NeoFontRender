package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputApiTest {
    @Test
    void higherPriorityDroneBlockNeutralizesPlayerAndFlightActions() {
        InputContext drone = InputContext.builder(id("drone"), 100)
                .claim(InputAction.CAMERA_LOOK_X, InputAction.CAMERA_TRANSLATE_Z)
                .block(InputAction.PLAYER_MOVE_FORWARD, InputAction.PLAYER_ATTACK,
                        InputAction.FLIGHT_PITCH, InputAction.FLIGHT_ROLL)
                .build();
        InputRegistration registration = InputApi.pushContext(drone);
        try {
            EnumMap<InputAction, InputValue> values = new EnumMap<>(InputAction.class);
            values.put(InputAction.CAMERA_LOOK_X, InputValue.axis(0.75F));
            values.put(InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(1.0F));
            values.put(InputAction.FLIGHT_PITCH, InputValue.axis(-0.8F));

            InputFrame frame = InputApi.publish(frame(1L), values);
            assertEquals(InputDisposition.CLAIM, frame.disposition(InputAction.CAMERA_LOOK_X));
            assertEquals(0.75F, frame.get(InputAction.CAMERA_LOOK_X).getAxis(), 0.0001F);
            assertEquals(InputDisposition.BLOCK, frame.disposition(InputAction.PLAYER_MOVE_FORWARD));
            assertEquals(0.0F, frame.get(InputAction.PLAYER_MOVE_FORWARD).getAxis(), 0.0001F);
            assertEquals(InputDisposition.BLOCK, frame.disposition(InputAction.FLIGHT_PITCH));
            assertEquals(0.0F, frame.get(InputAction.FLIGHT_PITCH).getAxis(), 0.0001F);
            assertEquals(id("drone"), frame.owner(InputAction.FLIGHT_PITCH));
        } finally {
            registration.close();
        }
    }

    @Test
    void resolvesEqualPrioritiesByNamespacedId() {
        InputRegistration alpha = InputApi.pushContext(InputContext.builder(id("alpha"), 5)
                .claim(InputAction.CAMERA_ZOOM).build());
        InputRegistration beta = InputApi.pushContext(InputContext.builder(id("beta"), 5)
                .block(InputAction.CAMERA_ZOOM).build());
        try {
            InputFrame frame = InputApi.publish(frame(2L), Collections.singletonMap(
                    InputAction.CAMERA_ZOOM, InputValue.axis(1.0F)));
            assertEquals(InputDisposition.CLAIM, frame.disposition(InputAction.CAMERA_ZOOM));
            assertEquals(id("alpha"), frame.owner(InputAction.CAMERA_ZOOM));
            assertEquals(1.0F, frame.get(InputAction.CAMERA_ZOOM).getAxis(), 0.0001F);
        } finally {
            beta.close();
            alpha.close();
        }
    }

    @Test
    void mapsAndMergesControllerSourcesWithDeadzoneAndDisconnectSafety() {
        ResourceLocation control = id("left_x");
        InputRegistration low = InputApi.registerDeviceSource(id("controller_low"), 1,
                ignored -> InputDeviceSample.builder(id("controller_low"))
                        .put(control, InputValue.axis(0.4F)).build());
        InputRegistration high = InputApi.registerDeviceSource(id("controller_high"), 2,
                ignored -> InputDeviceSample.builder(id("controller_high"))
                        .put(control, InputValue.axis(-0.8F)).build());
        InputRegistration binding = InputApi.registerBindingProvider(id("controller_binding"), 0,
                (ignored, sink) -> sink.bind(new InputBinding(control, InputAction.FLIGHT_ROLL,
                        0.2F, 1.0F, true)));
        try {
            InputFrame frame = InputApi.sample(frame(3L));
            // -0.8 after the 0.2 deadzone becomes -0.75; inversion yields +0.75.
            assertEquals(0.75F, frame.get(InputAction.FLIGHT_ROLL).getAxis(), 0.0001F);
            assertEquals(InputDisposition.PASS, frame.disposition(InputAction.FLIGHT_ROLL));
            assertFalse(frame.get(InputAction.FLIGHT_ROLL).isPressed());
            assertTrue(frame.get(InputAction.FLIGHT_ROLL).isDown());
        } finally {
            binding.close();
            high.close();
            low.close();
        }
    }

    @Test
    void focusLossPublishesNeutralFrameWithoutSamplingHeldControls() {
        InputRegistration device = InputApi.registerDeviceSource(id("focused_device"), 0,
                ignored -> InputDeviceSample.builder(id("focused_device"))
                        .put(id("look_x"), InputValue.axis(1.0F)).build());
        InputRegistration binding = InputApi.registerBindingProvider(id("focused_binding"), 0,
                (ignored, sink) -> sink.bind(new InputBinding(id("look_x"),
                        InputAction.CAMERA_LOOK_X)));
        try {
            InputFrame frame = InputApi.beginFrame(0.25F, false);
            assertFalse(frame.getContext().isGameFocused());
            assertEquals(InputFlushReason.FOCUS_LOST, frame.getContext().getFlushReason());
            assertEquals(0.0F, frame.get(InputAction.CAMERA_LOOK_X).getAxis(), 0.0001F);
        } finally {
            binding.close();
            device.close();
        }
    }

    @Test
    void flushReasonAndCurrentOwnersAreRetainedForDiagnostics() {
        InputRegistration registration = InputApi.pushContext(InputContext.builder(id("diagnostic"), 5)
                .claim(InputAction.CAMERA_LOOK_X).build());
        try {
            InputFrame frame = InputApi.flush(InputFlushReason.MODE_ENTER);
            assertEquals(InputFlushReason.MODE_ENTER, frame.getContext().getFlushReason());
            InputDiagnostics diagnostics = InputApi.diagnostics();
            assertEquals(InputFlushReason.MODE_ENTER, diagnostics.getFlushReason());
            assertEquals(id("diagnostic"), diagnostics.owner(InputAction.CAMERA_LOOK_X));
            assertTrue(diagnostics.getActiveContextIds().contains(id("diagnostic")));
        } finally {
            registration.close();
        }
    }

    @Test
    void deviceFailurePropagatesToCaller() {
        InputRegistration registration = InputApi.registerDeviceSource(id("failing_device"), 0,
                ignored -> { throw new IllegalStateException("device failure"); });
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> InputApi.sample(frame(9L)));
            assertEquals("device failure", error.getMessage());
        } finally {
            registration.close();
        }
    }

    @Test
    void registeringTheSameContextIdAtomicallyReplacesTheOldProvider() {
        ResourceLocation contextId = id("replace_context");
        InputRegistration oldRegistration = InputApi.pushContext(InputContext.builder(contextId, 5)
                .block(InputAction.PLAYER_MOVE_FORWARD).build());
        InputRegistration replacement = InputApi.pushContext(InputContext.builder(contextId, 5)
                .claim(InputAction.CAMERA_LOOK_X).build());
        try {
            InputFrame frame = InputApi.publish(frame(10L), Collections.singletonMap(
                    InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(1.0F)));
            assertEquals(InputDisposition.PASS,
                    frame.disposition(InputAction.PLAYER_MOVE_FORWARD));
            assertEquals(1.0F, frame.get(InputAction.PLAYER_MOVE_FORWARD).getAxis(), 0.0001F);
        } finally {
            replacement.close();
            oldRegistration.close();
        }
    }

    @Test
    void closingDroneContextRestoresPlayerMovementDisposition() {
        InputRegistration drone = InputApi.pushContext(InputContext.builder(id("drone_lifecycle"), 100)
                .block(InputAction.PLAYER_MOVE_FORWARD, InputAction.PLAYER_MOVE_STRAFE).build());
        InputFrame blocked = InputApi.publish(frame(11L), Collections.singletonMap(
                InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(1.0F)));
        assertEquals(InputDisposition.BLOCK,
                blocked.disposition(InputAction.PLAYER_MOVE_FORWARD));
        drone.close();

        InputFrame shoulder = InputApi.publish(frame(12L), Collections.singletonMap(
                InputAction.PLAYER_MOVE_FORWARD, InputValue.axis(1.0F)));
        assertEquals(InputDisposition.PASS,
                shoulder.disposition(InputAction.PLAYER_MOVE_FORWARD));
        assertEquals(1.0F, shoulder.get(InputAction.PLAYER_MOVE_FORWARD).getAxis(), 0.0001F);
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("uie_test", path); }
    private static InputFrameContext frame(long sampleId) {
        return new InputFrameContext(sampleId, 0.5F, 1.0D / 60.0D, true);
    }
}
