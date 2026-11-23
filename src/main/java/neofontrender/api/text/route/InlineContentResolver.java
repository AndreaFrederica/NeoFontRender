package neofontrender.api.text.route;

import neofontrender.text.InlineContent;

/** Resolves deferred inline descriptors before a render route performs layout. */
public interface InlineContentResolver {
    String id();
    default int priority() { return 0; }
    default boolean isEnabled() { return true; }
    boolean supports(InlineContent content);

    /** Return the input descriptor while content is still loading or unavailable. */
    InlineContent resolve(InlineContent content);
}
