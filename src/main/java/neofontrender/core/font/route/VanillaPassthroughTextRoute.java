package neofontrender.core.font.route;

import neofontrender.api.text.route.TextRenderRoute;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;

/** Explicit terminal route for strings which the original FontRenderer can own unchanged. */
final class VanillaPassthroughTextRoute implements TextRenderRoute {
    static final VanillaPassthroughTextRoute INSTANCE = new VanillaPassthroughTextRoute();
    static final String ID = "neofontrender:vanilla_passthrough";

    private VanillaPassthroughTextRoute() {}

    @Override public String id() { return ID; }
    @Override public int priority() { return -1000; }
    @Override public boolean supports(TextRenderRouteRequest request) { return true; }
    @Override public TextRenderRouteLayout layout(TextRenderRouteRequest request) {
        return new VanillaPassthroughLayout(request.font(), request.source(),
                request.structuredText(), request.argb(), request.shadow(), "selected");
    }
}
