package neofontrender.addons.inline;

import neofontrender.api.client.font.BuiltinFontRegistry;

/** Namespaced font resources contributed by Revo UI. */
public final class EmbeddedContentFonts {
    public static final String FIRA_MATH_ID = "neofontrender_ui_enhancements:fira_math";
    public static final String FIRA_MATH_FAMILY = "Fira Math";
    public static final String FIRA_MATH_LOCATION =
            "neofontrender_ui_enhancements:fonts/fira_math-regular.otf";

    private EmbeddedContentFonts() {}

    public static void register() {
        // Formula default is selectable but should not become a fallback for every ordinary line.
        BuiltinFontRegistry.register(FIRA_MATH_ID, FIRA_MATH_FAMILY,
                FIRA_MATH_LOCATION, false);
    }
}
