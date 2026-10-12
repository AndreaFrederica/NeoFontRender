package neofontrender.addons.api.input;

/** Supplies a context for a frame. Return {@code null} when the mode is inactive. */
@FunctionalInterface
public interface InputContextProvider {
    InputContext context(InputFrameContext frame);
}
