package neofontrender.addons.controller.sdl;

import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputBinding;
import neofontrender.addons.api.input.InputBindingProvider;
import neofontrender.addons.api.input.InputBindingSink;
import neofontrender.addons.api.input.InputFrameContext;
import neofontrender.addons.controller.ControllerBindingSpec;
import neofontrender.addons.controller.ControllerBindings;
import neofontrender.addons.controller.ControllerConfig;
import neofontrender.addons.controller.ControllerInputMode;

import java.util.Objects;
import java.util.function.Supplier;

/** Default gamepad-to-intent mapping. It only declares mappings; UIE owns all routing decisions. */
public final class SdlBindingProvider implements InputBindingProvider {
    private final Supplier<ControllerInputMode> mode;

    public SdlBindingProvider() {
        this(ControllerInputMode::current);
    }

    SdlBindingProvider(Supplier<ControllerInputMode> mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public void bind(InputFrameContext frame, InputBindingSink sink) {
        ControllerInputMode activeMode = mode.get();
        for (ControllerBindingSpec spec : ControllerBindings.all()) {
            if (!spec.isBound() || !activeMode.accepts(spec.getAction())) continue;
            sink.bind(new InputBinding(spec.getControl(), spec.getAction(),
                    ControllerConfig.deadzone(), ControllerBindings.effectiveScale(spec),
                    ControllerBindings.effectiveInverted(spec)));
        }
    }
}
