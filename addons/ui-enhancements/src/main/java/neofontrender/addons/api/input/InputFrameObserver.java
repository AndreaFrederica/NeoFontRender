package neofontrender.addons.api.input;

/** Receives the immutable routed frame after all contexts have been resolved. */
@FunctionalInterface
public interface InputFrameObserver {
    void onInputFrame(InputFrame frame);
}
