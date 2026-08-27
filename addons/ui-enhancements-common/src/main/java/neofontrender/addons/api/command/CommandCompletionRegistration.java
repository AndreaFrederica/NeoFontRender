package neofontrender.addons.api.command;

/** Idempotent handle for one command-completion provider registration. */
@FunctionalInterface
public interface CommandCompletionRegistration extends AutoCloseable {
    @Override
    void close();
}
