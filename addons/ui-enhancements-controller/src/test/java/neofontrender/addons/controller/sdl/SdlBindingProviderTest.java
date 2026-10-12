package neofontrender.addons.controller.sdl;

import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputBinding;
import neofontrender.addons.api.input.InputFrameContext;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputDeviceSample;
import neofontrender.addons.api.input.InputRegistration;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.controller.ControllerConfig;
import neofontrender.addons.controller.ControllerInputMode;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SdlBindingProviderTest {
    @AfterEach
    void restoreDefaults() {
        ControllerConfig.setDeadzone(ControllerConfig.DEFAULT_DEADZONE);
        ControllerConfig.setLookSensitivity(ControllerConfig.DEFAULT_LOOK_SENSITIVITY);
        ControllerConfig.setFlightSensitivity(ControllerConfig.DEFAULT_FLIGHT_SENSITIVITY);
        ControllerConfig.setInvertLookX(false);
        ControllerConfig.setInvertLookY(false);
        ControllerConfig.setInvertFlightPitch(false);
        ControllerConfig.setInvertFlightYaw(false);
        ControllerConfig.setInvertFlightRoll(false);
    }

    @Test
    void exposesCameraAndFlightIntentWithoutTouchingMixins() {
        List<InputBinding> bindings = new ArrayList<>();
        new SdlBindingProvider(() -> ControllerInputMode.GAMEPLAY).bind(
                new InputFrameContext(1L, 0.0F, 0.0D, true), bindings::add);

        assertTrue(bindings.stream().anyMatch(binding ->
                binding.getControl().equals(ControllerControls.RIGHT_STICK_X)
                        && binding.getAction() == InputAction.CAMERA_LOOK_X));
        assertTrue(bindings.stream().anyMatch(binding ->
                binding.getControl().equals(ControllerControls.RIGHT_TRIGGER)
                && binding.getAction() == InputAction.CAMERA_TRANSLATE_Y));

        bindings.clear();
        new SdlBindingProvider(() -> ControllerInputMode.FLIGHT).bind(
                new InputFrameContext(2L, 0.0F, 0.0D, true), bindings::add);
        assertTrue(bindings.stream().anyMatch(binding ->
                binding.getControl().equals(ControllerControls.LEFT_STICK_Y)
                        && binding.getAction() == InputAction.FLIGHT_PITCH));
        assertTrue(bindings.stream().anyMatch(binding ->
                binding.getControl().equals(ControllerControls.TRIGGER_RUDDER)
                        && binding.getAction() == InputAction.FLIGHT_RUDDER));
    }

    @Test
    void appliesLiveSensitivityAndAxisInversion() {
        ControllerConfig.setDeadzone(0.0F);
        ControllerConfig.setLookSensitivity(2.0F);
        ControllerConfig.setFlightSensitivity(0.5F);
        ControllerConfig.setInvertLookX(true);
        ControllerConfig.setInvertFlightRoll(false);

        InputDeviceSample sample = InputDeviceSample.builder(
                        new ResourceLocation("test", "controller"))
                .put(ControllerControls.RIGHT_STICK_X, InputValue.axis(0.4F))
                .build();
        InputRegistration device = InputApi.registerDeviceSource(
                new ResourceLocation("test", "device"), 100, frame -> sample);
        InputRegistration bindings = InputApi.registerBindingProvider(
                new ResourceLocation("test", "bindings"), 100,
                new SdlBindingProvider(() -> ControllerInputMode.GAMEPLAY));
        try {
            neofontrender.addons.api.input.InputFrame frame = InputApi.sample(
                    new InputFrameContext(2L, 0.0F, 0.0D, true));
            assertEquals(-0.8F, frame.get(InputAction.CAMERA_LOOK_X).getAxis(), 1.0E-6F);
            assertEquals(0.0F, frame.get(InputAction.FLIGHT_ROLL).getAxis(), 1.0E-6F);
        } finally {
            bindings.close();
            device.close();
        }
    }
}
