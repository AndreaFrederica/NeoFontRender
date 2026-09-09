package neofontrender.api.text.animation;

/** Lifetime of one explicit logical text-animation instance. */
@FunctionalInterface
public interface TextAnimationScope extends AutoCloseable {
    @Override
    void close();
}
