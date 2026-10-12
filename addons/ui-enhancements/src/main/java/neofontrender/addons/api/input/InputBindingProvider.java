package neofontrender.addons.api.input;

/** Supplies active physical-control mappings. It may vary mappings by focus or active mode. */
@FunctionalInterface
public interface InputBindingProvider {
    void bind(InputFrameContext frame, InputBindingSink sink);
}
