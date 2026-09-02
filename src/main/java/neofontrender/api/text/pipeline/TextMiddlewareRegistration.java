package neofontrender.api.text.pipeline;

/** A removable text-pipeline registration. Closing an old handle never removes its replacement. */
@FunctionalInterface
public interface TextMiddlewareRegistration extends AutoCloseable {
    @Override
    void close();
}
