package neofontrender.addons.ui;

/** Lifecycle contract for an independently configurable UI enhancement. */
public interface UiEnhancementModule {
    /** Loads configuration before event handlers become active. */
    void preInit();

    /** Registers the module's settings and runtime hooks. */
    void init();
}
