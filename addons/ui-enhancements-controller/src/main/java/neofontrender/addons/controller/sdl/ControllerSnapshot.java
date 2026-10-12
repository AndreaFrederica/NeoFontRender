package neofontrender.addons.controller.sdl;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.input.InputDeviceSample;
import neofontrender.addons.api.input.InputValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable linear physical-input snapshot used by the controller workbench. */
public final class ControllerSnapshot {
    private static final ControllerSnapshot DISCONNECTED = new ControllerSnapshot(
            ControllerControls.DISCONNECTED_DEVICE, "", false,
            Collections.emptyMap(), 0L);

    private final ResourceLocation deviceId;
    private final String deviceName;
    private final boolean gamepad;
    private final Map<ResourceLocation, InputValue> controls;
    private final long sampledAtNanos;

    public ControllerSnapshot(ResourceLocation deviceId, String deviceName, boolean gamepad,
                              Map<ResourceLocation, InputValue> controls, long sampledAtNanos) {
        this.deviceId = deviceId;
        this.deviceName = deviceName == null ? "" : deviceName;
        this.gamepad = gamepad;
        this.controls = Collections.unmodifiableMap(new LinkedHashMap<>(controls));
        this.sampledAtNanos = sampledAtNanos;
    }

    public static ControllerSnapshot disconnected() { return DISCONNECTED; }
    public boolean isConnected() { return !ControllerControls.DISCONNECTED_DEVICE.equals(deviceId); }
    public ResourceLocation getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public boolean isGamepad() { return gamepad; }
    public long getSampledAtNanos() { return sampledAtNanos; }
    public Map<ResourceLocation, InputValue> controls() { return controls; }
    public InputValue get(ResourceLocation control) {
        return controls.getOrDefault(control, InputValue.NEUTRAL);
    }

    InputDeviceSample toDeviceSample() {
        InputDeviceSample.Builder builder = InputDeviceSample.builder(deviceId);
        controls.forEach(builder::put);
        return builder.build();
    }
}
