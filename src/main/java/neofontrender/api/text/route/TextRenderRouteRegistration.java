package neofontrender.api.text.route;

/** Lifetime handle returned by the route registry. */
public interface TextRenderRouteRegistration extends AutoCloseable {
    @Override
    void close();
}
