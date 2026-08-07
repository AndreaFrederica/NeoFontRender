package neofontrender.addons.tips;

import neofontrender.api.i18n.JsonLangLoader;

/**
 * Tips-specific i18n helper. Registers the required namespaces with
 * {@link JsonLangLoader} and provides a convenience translate method.
 */
public final class TipsI18n {
    private TipsI18n() {}

    public static void init() {
        JsonLangLoader.INSTANCE.registerNamespace("neofontrender_ui_enhancements");
        JsonLangLoader.INSTANCE.registerNamespace("tipsmod");
    }

    public static String translate(String key) {
        return JsonLangLoader.INSTANCE.translate(key);
    }
}
