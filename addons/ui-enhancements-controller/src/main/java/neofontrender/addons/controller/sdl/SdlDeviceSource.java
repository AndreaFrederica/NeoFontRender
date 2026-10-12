package neofontrender.addons.controller.sdl;

import neofontrender.addons.api.input.InputDeviceSample;
import neofontrender.addons.api.input.InputDeviceSource;
import neofontrender.addons.api.input.InputFrameContext;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/** Bridges current SDL device state into UIE without translating controller axes into mouse deltas. */
public final class SdlDeviceSource implements InputDeviceSource {
    private final SdlDeviceManager manager;
    private final Logger logger;

    public SdlDeviceSource(SdlDeviceManager manager, Logger logger) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public InputDeviceSample sample(InputFrameContext frame) {
        if (!frame.isGameFocused() || frame.getFlushReason() != null) {
            return InputDeviceSample.builder(ControllerControls.DISCONNECTED_DEVICE).build();
        }
        try {
            return manager.sample();
        } catch (RuntimeException error) {
            logger.error("SDL controller sampling failed; publishing a neutral input sample", error);
            return InputDeviceSample.builder(ControllerControls.DISCONNECTED_DEVICE).build();
        }
    }
}
