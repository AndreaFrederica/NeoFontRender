package neofontrender.text.pipeline;

import java.util.Collection;
import java.util.Collections;

/** ServiceLoader entry point shared by Minecraft and the standalone laboratory. */
public interface TextPipelinePlugin {
    String id();

    default Collection<? extends StructuredTextMiddleware> structuredMiddlewares() {
        return Collections.emptyList();
    }

    default Collection<? extends LineBreakOpportunityProvider> lineBreakProviders() {
        return Collections.emptyList();
    }
}
