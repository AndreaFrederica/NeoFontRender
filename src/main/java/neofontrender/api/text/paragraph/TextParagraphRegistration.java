package neofontrender.api.text.paragraph;

/** Handle for removing one paragraph provider registration. */
@FunctionalInterface
public interface TextParagraphRegistration extends AutoCloseable {
    @Override void close();
}
