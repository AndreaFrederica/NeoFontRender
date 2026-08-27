package neofontrender.addons.outlines;

import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class BlockOutlineConfig {
    static boolean enabled = true;
    static float globalLineWidth = 2.0F;
    static int globalColor = 0x66000000;
    static List<String> blockOverrides = Collections.emptyList();
    static boolean noHarvestEnabled;
    static float noHarvestLineWidth = 2.0F;
    static int noHarvestColor = 0x66FF0000;
    static List<String> noHarvestOverrides = Collections.emptyList();

    private BlockOutlineConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("outlines.enabled", true, "Master switch for configurable block-selection outlines.")
                .define("outlines.globalLineWidth", 2.0D, "Default selection-outline width (0-1000).")
                .define("outlines.globalColor", "#66000000", "Default selection-outline color in #AARRGGBB format.")
                .define("outlines.blockOverrides", Collections.emptyList(),
                        "Per-block rules: modid:block[:meta]=width;#AARRGGBB")
                .define("outlines.noHarvest.enabled", false,
                        "Use a distinct outline when the player cannot harvest the selected block.")
                .define("outlines.noHarvest.lineWidth", 2.0D, "Default non-harvestable outline width (0-1000).")
                .define("outlines.noHarvest.color", "#66FF0000",
                        "Default non-harvestable outline color in #AARRGGBB format.")
                .define("outlines.noHarvest.blockOverrides", Collections.emptyList(),
                        "Per-block non-harvestable rules: modid:block[:meta]=width;#AARRGGBB");
        enabled = file.getBoolean("outlines.enabled", true);
        globalLineWidth = (float) file.getDouble("outlines.globalLineWidth", 2.0D, 0.0D, 1000.0D);
        globalColor = parseColor(file.getString("outlines.globalColor", "#66000000"), 0x66000000);
        blockOverrides = mutable(file.getStringList("outlines.blockOverrides", Collections.emptyList()));
        noHarvestEnabled = file.getBoolean("outlines.noHarvest.enabled", false);
        noHarvestLineWidth = (float) file.getDouble("outlines.noHarvest.lineWidth", 2.0D, 0.0D, 1000.0D);
        noHarvestColor = parseColor(file.getString("outlines.noHarvest.color", "#66FF0000"), 0x66FF0000);
        noHarvestOverrides = mutable(file.getStringList("outlines.noHarvest.blockOverrides", Collections.emptyList()));
        BlockOutlineResolver.reload();
        file.save();
    }

    static void save() {
        BlockOutlineResolver.reload();
        UiEnhancementsConfig.file().set("outlines.enabled", enabled)
                .set("outlines.globalLineWidth", (double) globalLineWidth)
                .set("outlines.globalColor", formatColor(globalColor))
                .set("outlines.blockOverrides", new ArrayList<>(blockOverrides))
                .set("outlines.noHarvest.enabled", noHarvestEnabled)
                .set("outlines.noHarvest.lineWidth", (double) noHarvestLineWidth)
                .set("outlines.noHarvest.color", formatColor(noHarvestColor))
                .set("outlines.noHarvest.blockOverrides", new ArrayList<>(noHarvestOverrides))
                .save();
    }

    static Snapshot snapshot() { return new Snapshot(); }

    static List<String> parseEditorRules(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String entry : value.split("[,\\r\\n]+")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    static String editorRules(List<String> values) { return String.join(", ", values); }

    private static List<String> mutable(List<String> values) { return new ArrayList<>(values); }

    private static int parseColor(String value, int fallback) {
        try {
            String hex = value == null ? "" : value.trim();
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() != 8) return fallback;
            return (int) Long.parseLong(hex, 16);
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.warn("Invalid block outline color: {}", value);
            return fallback;
        }
    }

    private static String formatColor(int value) { return String.format("#%08X", value); }

    static final class Snapshot {
        private final boolean enabledValue = enabled;
        private final float globalLineWidthValue = globalLineWidth;
        private final int globalColorValue = globalColor;
        private final List<String> blockOverridesValue = mutable(blockOverrides);
        private final boolean noHarvestEnabledValue = noHarvestEnabled;
        private final float noHarvestLineWidthValue = noHarvestLineWidth;
        private final int noHarvestColorValue = noHarvestColor;
        private final List<String> noHarvestOverridesValue = mutable(noHarvestOverrides);

        void restore() {
            enabled = enabledValue;
            globalLineWidth = globalLineWidthValue;
            globalColor = globalColorValue;
            blockOverrides = mutable(blockOverridesValue);
            noHarvestEnabled = noHarvestEnabledValue;
            noHarvestLineWidth = noHarvestLineWidthValue;
            noHarvestColor = noHarvestColorValue;
            noHarvestOverrides = mutable(noHarvestOverridesValue);
            BlockOutlineResolver.reload();
        }
    }
}
