package neofontrender.core.font.route;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
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
}
