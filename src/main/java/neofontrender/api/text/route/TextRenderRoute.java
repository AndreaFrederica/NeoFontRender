package neofontrender.api.text.route;

/** One registered backend route consuming the shared structured-text protocol. */
public interface TextRenderRoute {
    String id();

    default int priority() {
        return 0;
    }

    default boolean isEnabled() {
        return true;
    }

    boolean supports(TextRenderRouteRequest request);

    TextRenderRouteLayout layout(TextRenderRouteRequest request);
}
