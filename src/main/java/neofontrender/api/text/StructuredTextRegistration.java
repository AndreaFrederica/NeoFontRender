package neofontrender.api.text;

/** A removable syntax, structured-middleware, or inline-resolver registration. */
@FunctionalInterface
public interface StructuredTextRegistration extends AutoCloseable {
    @Override
    void close();
}
