package neofontrender.api.text;

import neofontrender.api.text.route.InlineContentResolver;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.text.StructuredText;
import neofontrender.text.pipeline.TextPipelinePlugin;
import neofontrender.text.syntax.TextSyntaxProvider;

import java.util.List;

/** Public registry and parser facade for the shared structured-text protocol. */
public final class StructuredTextApi {
    public static final int API_VERSION = 1;

    private StructuredTextApi() {}

    public static StructuredTextRegistration register(TextSyntaxProvider provider) {
        return StructuredTextRuntime.register(provider);
    }

    public static StructuredTextRegistration register(TextPipelinePlugin plugin) {
        return StructuredTextRuntime.register(plugin);
    }

    public static StructuredTextRegistration register(InlineContentResolver resolver) {
        return StructuredTextRuntime.register(resolver);
    }

    public static StructuredText parse(String source) {
        return StructuredTextRuntime.parse(source);
    }

    public static List<String> syntaxProviderIds() {
        return StructuredTextRuntime.providerIds();
    }

    public static List<String> middlewareIds() {
        return StructuredTextRuntime.middlewareIds();
    }

    public static List<String> lineBreakProviderIds() {
        return StructuredTextRuntime.lineBreakProviderIds();
    }

    public static long revision() {
        return StructuredTextRuntime.revision();
    }

    public static void invalidate() {
        StructuredTextRuntime.invalidate();
    }
}
