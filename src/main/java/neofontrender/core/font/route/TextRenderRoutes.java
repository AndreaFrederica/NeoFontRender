package neofontrender.core.font.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.api.text.route.TextRenderRouteRequest;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.text.SourceMap;
import neofontrender.text.StructuredText;

import java.util.Collections;

/** Installs NFR's built-in route adapters. */
public final class TextRenderRoutes {
    private static boolean initialized;

    private TextRenderRoutes() {}

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        TextRenderRouteApi.register(ModernStructuredTextRoute.INSTANCE);
        TextRenderRouteApi.register(VanillaCompatibilityTextRoute.INSTANCE);
        TextRenderRouteApi.register(VanillaPassthroughTextRoute.INSTANCE);
    }

    public static TextRenderRouteLayout passthrough(FontRenderer font, String source,
                                                    int argb, boolean shadow, String reason) {
        String value = source == null ? "" : source;
        int[] boundaries = new int[value.length() + 1];
        for (int index = 0; index < boundaries.length; index++) boundaries[index] = index;
        StructuredText structured = new StructuredText(value, value, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                new SourceMap(value.length(), boundaries, boundaries));
        return new VanillaPassthroughLayout(font, value, structured, argb, shadow, reason);
    }

    /** Scoped cosmic layout used by the F3 overlay when it selects its own engine. */
    public static TextRenderRouteLayout scopedCosmic(net.minecraft.client.gui.FontRenderer font,
                                                     String source, int argb, boolean shadow) {
        TextRenderBackend backend = FontManager.INSTANCE.getScopedCosmicBackend();
        if (backend == null || !backend.isReady()) return null;
        String value = source == null ? "" : source;
        TextRenderRouteRequest request = new TextRenderRouteRequest(font, value,
                StructuredTextRuntime.parse(value), NeofontrenderConfig.fontSize(),
                argb, shadow, false);
        return new ModernStructuredRouteLayout(request, backend);
    }
    /** Handled layout for engine=compat: vanilla draws, the compat leaf hooks supply glyphs. */
    public static TextRenderRouteLayout compat(FontRenderer font, String source,
                                               int argb, boolean shadow) {
        String value = source == null ? "" : source;
        int[] boundaries = new int[value.length() + 1];
        for (int index = 0; index < boundaries.length; index++) boundaries[index] = index;
        StructuredText structured = new StructuredText(value, value, Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                new SourceMap(value.length(), boundaries, boundaries));
        return new CompatRouteLayout(font, value, structured, argb, shadow);
    }
}
