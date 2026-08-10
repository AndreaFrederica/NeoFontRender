package neofontrender.addons.api.input;

/** Idempotent registration handle returned by {@link InputApi}. */
public interface InputRegistration extends AutoCloseable {
    @Override
    void close();
}
