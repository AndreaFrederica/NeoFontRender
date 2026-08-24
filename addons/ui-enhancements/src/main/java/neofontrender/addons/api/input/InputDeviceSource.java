package neofontrender.addons.api.input;

/** Samples one physical or virtual input device. Return an empty sample when disconnected. */
@FunctionalInterface
public interface InputDeviceSource {
    InputDeviceSample sample(InputFrameContext frame);
}
