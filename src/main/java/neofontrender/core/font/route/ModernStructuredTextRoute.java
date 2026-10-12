package neofontrender.core.font.route;

import neofontrender.api.text.route.TextRenderRoute;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.TextRenderBackend;

/** Configured AWT/Cosmic route. Availability never selects the lazy API fallback. */
final class ModernStructuredTextRoute implements TextRenderRoute {
    static final ModernStructuredTextRoute INSTANCE = new ModernStructuredTextRoute();
    static final String ID = "neofontrender:modern_structured";

    private ModernStructuredTextRoute() {}

    @Override public String id() { return ID; }
    @Override public int priority() { return 200; }

    @Override
    public boolean supports(TextRenderRouteRequest request) {
        if (NeofontrenderConfig.useVanillaEngine()) return false;
        FontManager manager = FontManager.INSTANCE;
        if (!manager.isCosmicActive() && !manager.isSfrActive()
                && !manager.isTextBackendActive()) return false;
        TextRenderBackend backend = manager.getModernTextBackend();
        return backend != null && backend.isReady();
    }

    @Override
    public TextRenderRouteLayout layout(TextRenderRouteRequest request) {
        return new ModernStructuredRouteLayout(request,
                FontManager.INSTANCE.getModernTextBackend());
    }
}
