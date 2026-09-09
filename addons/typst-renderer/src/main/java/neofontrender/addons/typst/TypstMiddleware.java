package neofontrender.addons.typst;

import neofontrender.api.text.StructuredTextApi;
import neofontrender.api.text.StructuredTextRegistration;
import neofontrender.typst.pipeline.TypstPipelinePlugin;

/** Idempotent registration point matching UIE's middleware lifecycle. */
final class TypstMiddleware {
    private static boolean initialized;
    private static boolean shutdownHookInstalled;
    private static TypstPipelinePlugin plugin;
    private static StructuredTextRegistration registration;

    private TypstMiddleware() {}

    static TypstPipelinePlugin plugin() { return plugin; }

    static synchronized void initialize() {
        if (initialized) return;
        TypstPipelinePlugin created = new TypstPipelinePlugin(new TypstPipelinePlugin.Config(
                () -> TypstConfig.libraryDirectory().toPath(), TypstConfig::enabled,
                () -> TypstConfig.oversample(), TypstConfig::maxToken,
                StructuredTextApi::invalidate));
        try {
            registration = StructuredTextApi.register(created);
            plugin = created;
            initialized = true;
        } catch (RuntimeException | Error failure) {
            created.close();
            throw failure;
        }
    }

    static synchronized void initializeShutdownHook() {
        if (shutdownHookInstalled) return;
        shutdownHookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread(TypstMiddleware::shutdown,
                "NFR Typst renderer shutdown"));
    }

    private static synchronized void shutdown() {
        if (registration != null) registration.close();
        registration = null;
        if (plugin != null) plugin.close();
        plugin = null;
        initialized = false;
    }
}
