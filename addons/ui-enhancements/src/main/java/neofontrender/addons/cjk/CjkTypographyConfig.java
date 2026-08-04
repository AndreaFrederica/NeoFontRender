package neofontrender.addons.cjk;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.Locale;

final class CjkTypographyConfig {
    static final String ENGINE_TIQIAN = "tiqian";
    static final String ENGINE_LEGACY = "legacy";

    static String engine = ENGINE_TIQIAN;

    private CjkTypographyConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("cjkTypography.engine", ENGINE_TIQIAN,
                "CJK paragraph engine: tiqian (experimental, Simplified Chinese) or legacy.");
        engine = normalize(file.getString("cjkTypography.engine", ENGINE_TIQIAN));
        file.save();
    }

    static void save() {
        engine = normalize(engine);
        UiEnhancementsConfig.file().set("cjkTypography.engine", engine).save();
        TiqianParagraphProvider.INSTANCE.clearCache();
    }

    static boolean tiqianEnabled() {
        return ENGINE_TIQIAN.equals(engine);
    }

    static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return ENGINE_LEGACY.equals(normalized) ? ENGINE_LEGACY : ENGINE_TIQIAN;
    }
}
