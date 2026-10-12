package neofontrender.text.pipeline;

import neofontrender.text.StructuredText;

import java.util.List;

/** Game-independent paragraph-stage provider in plain-text boundary coordinates. */
public interface LineBreakOpportunityProvider {
    String id();
    default int priority() { return 0; }
    default boolean isEnabled() { return true; }
    List<Integer> opportunities(StructuredText text);
}
