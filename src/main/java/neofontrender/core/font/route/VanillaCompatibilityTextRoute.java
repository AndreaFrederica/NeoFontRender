package neofontrender.core.font.route;

import neofontrender.api.text.route.TextRenderRoute;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.config.NeofontrenderConfig;

/** Structured adapter used when contributed syntax must still render with FontRenderer. */
final class VanillaCompatibilityTextRoute implements TextRenderRoute {
    static final VanillaCompatibilityTextRoute INSTANCE = new VanillaCompatibilityTextRoute();
    static final String ID = "neofontrender:vanilla_compatibility";

    private VanillaCompatibilityTextRoute() {}

    @Override public String id() { return ID; }
    @Override public int priority() { return 100; }
    @Override public boolean supports(TextRenderRouteRequest request) {
        return request.requiresStructuredRendering() || NeofontrenderConfig.fixCjkLineBreak();
    }
    @Override public TextRenderRouteLayout layout(TextRenderRouteRequest request) {
        return new VanillaCompatibilityRouteLayout(request);
    }
}
