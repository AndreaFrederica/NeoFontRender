package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable raw-control snapshot produced by one keyboard, mouse, controller, or adapter. */
public final class InputDeviceSample {
    private final ResourceLocation deviceId;
    private final Map<ResourceLocation, InputValue> controls;

    private InputDeviceSample(ResourceLocation deviceId, Map<ResourceLocation, InputValue> controls) {
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId");
        this.controls = Collections.unmodifiableMap(new LinkedHashMap<>(controls));
    }

    public ResourceLocation getDeviceId() { return deviceId; }
    public InputValue get(ResourceLocation control) {
        return controls.getOrDefault(control, InputValue.NEUTRAL);
    }
    public Map<ResourceLocation, InputValue> controls() { return controls; }

    public static Builder builder(ResourceLocation deviceId) { return new Builder(deviceId); }

    public static final class Builder {
        private final ResourceLocation deviceId;
        private final Map<ResourceLocation, InputValue> controls = new LinkedHashMap<>();

        private Builder(ResourceLocation deviceId) {
            this.deviceId = Objects.requireNonNull(deviceId, "deviceId");
        }

        public Builder put(ResourceLocation control, InputValue value) {
            controls.put(Objects.requireNonNull(control, "control"),
                    Objects.requireNonNull(value, "value"));
            return this;
        }

        public InputDeviceSample build() { return new InputDeviceSample(deviceId, controls); }
    }
}
