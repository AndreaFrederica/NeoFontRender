package neofontrender.addons.api.input;

/** Collector used by an {@link InputBindingProvider} during a frame. */
@FunctionalInterface
public interface InputBindingSink {
    void bind(InputBinding binding);
}
