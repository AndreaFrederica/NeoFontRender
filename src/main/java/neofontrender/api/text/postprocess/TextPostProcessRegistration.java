package neofontrender.api.text.postprocess;

/** A removable post-processor registration. */
@FunctionalInterface
public interface TextPostProcessRegistration extends AutoCloseable {
    @Override
    void close();
}
