package neofontrender.api.color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;

/** Registry and selector for mod-provided legacy text color palettes. */
public final class TextColorPaletteRegistry {
    public static final String AUTO = "auto";
    public static final String VANILLA = "vanilla";
    public static final String RUNTIME = "runtime";
    public static final String CUSTOM = "custom";
    private static final int COLOR_COUNT = 32;
    private static final Map<String, TextColorPaletteProvider> PROVIDERS = new LinkedHashMap<>();
    private static volatile int[] customColorCodes = vanillaColorCodes();
    private static volatile long revision;

    static {
        register(new FixedProvider(VANILLA, "Vanilla", -1000, vanillaColorCodes()));
        register(new TextColorPaletteProvider() {
            @Override
            public String id() {
                return RUNTIME;
            }

            @Override
            public String displayName() {
                return "Runtime FontRenderer";
            }

            @Override
            public int priority() {
                return 100;
            }

            @Override
            public int[] colorCodes(int[] runtimeColorCodes) {
                return runtimeColorCodes;
            }
        });
        register(new TextColorPaletteProvider() {
            @Override
            public String id() {
                return CUSTOM;
            }

            @Override
            public String displayName() {
                return "Custom";
            }

            @Override
            public int priority() {
                // Custom colors are only activated when explicitly selected.
                return -500;
            }

            @Override
            public int[] colorCodes(int[] runtimeColorCodes) {
                return customColorCodes;
            }
        });
    }

    private TextColorPaletteRegistry() {
    }

    public static synchronized void register(TextColorPaletteProvider provider) {
        if (provider == null) throw new IllegalArgumentException("Palette provider must not be null");
        String id = normalizeId(provider.id());
        if (AUTO.equals(id)) throw new IllegalArgumentException("'auto' is reserved for palette selection");
        if (PROVIDERS.containsKey(id)) {
            throw new IllegalArgumentException("Palette provider is already registered: " + id);
        }
        PROVIDERS.put(id, provider);
        revision++;
    }

    public static synchronized List<String> providerIds() {
        List<String> ids = new ArrayList<>();
        ids.add(AUTO);
        ids.addAll(PROVIDERS.keySet());
        return Collections.unmodifiableList(ids);
    }

    public static synchronized String displayName(String id) {
        String normalized = normalizeSelection(id);
        if (AUTO.equals(normalized)) return "Automatic";
        TextColorPaletteProvider provider = PROVIDERS.get(normalized);
        return provider == null ? normalized : provider.displayName();
    }

    /** Updates the palette returned by the built-in {@value #CUSTOM} provider. */
    public static void setCustomColorCodes(int[] colorCodes) {
        int[] normalized = normalizeColorCodes(colorCodes);
        if (!Arrays.equals(customColorCodes, normalized)) {
            customColorCodes = normalized;
            revision++;
        }
    }

    public static int[] customColorCodes() {
        return customColorCodes.clone();
    }

    /** Monotonic value used by renderers to avoid resolving an unchanged palette every draw. */
    public static long revision() {
        return revision;
    }

    /** Notify renderers after a provider's availability or internally generated colors change. */
    public static void invalidate() {
        revision++;
    }

    /** Resolves a configured provider against the current FontRenderer palette snapshot. */
    public static int[] resolve(String selectedProvider, int[] runtimeColorCodes) {
        TextColorPaletteProvider provider;
        synchronized (TextColorPaletteRegistry.class) {
            String selected = normalizeSelection(selectedProvider);
            provider = AUTO.equals(selected) ? automaticProvider() : PROVIDERS.get(selected);
            if (provider == null || !provider.isAvailable()) {
                provider = automaticProvider();
            }
        }
        int[] runtimeCopy = runtimeColorCodes == null ? null : runtimeColorCodes.clone();
        int[] supplied = provider == null ? null : provider.colorCodes(runtimeCopy);
        return normalizeColorCodes(supplied);
    }

    public static String normalizeSelection(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? AUTO : normalized;
    }

    public static int[] vanillaColorCodes() {
        int[] codes = new int[COLOR_COUNT];
        for (int index = 0; index < COLOR_COUNT; index++) {
            int intensity = (index >> 3 & 1) * 85;
            int red = (index >> 2 & 1) * 170 + intensity;
            int green = (index >> 1 & 1) * 170 + intensity;
            int blue = (index & 1) * 170 + intensity;
            if (index == 6) red += 85;
            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            codes[index] = red << 16 | green << 8 | blue;
        }
        return codes;
    }

    /**
     * Returns an independent 32-entry RGB palette. A 16-entry palette receives Minecraft-style
     * shadow colors; null or shorter inputs fall back to the vanilla palette.
     */
    public static int[] normalizeColorCodes(int[] supplied) {
        int[] normalized = vanillaColorCodes();
        if (supplied == null || supplied.length < 16) return normalized;
        int copied = Math.min(COLOR_COUNT, supplied.length);
        for (int index = 0; index < copied; index++) {
            normalized[index] = supplied[index] & 0xFFFFFF;
        }
        if (copied < COLOR_COUNT) {
            for (int index = 0; index < 16; index++) {
                int color = normalized[index];
                normalized[index + 16] = ((color >> 16 & 255) / 4) << 16
                        | ((color >> 8 & 255) / 4) << 8
                        | (color & 255) / 4;
            }
        }
        return normalized;
    }

    private static TextColorPaletteProvider automaticProvider() {
        return PROVIDERS.values().stream()
                .filter(TextColorPaletteProvider::isAvailable)
                .max(Comparator.comparingInt(TextColorPaletteProvider::priority))
                .orElse(PROVIDERS.get(VANILLA));
    }

    private static String normalizeId(String id) {
        String normalized = normalizeSelection(id);
        if (!normalized.matches("[a-z0-9_.:-]+")) {
            throw new IllegalArgumentException("Invalid palette provider id: " + id);
        }
        return normalized;
    }

    private static final class FixedProvider implements TextColorPaletteProvider {
        private final String id;
        private final String displayName;
        private final int priority;
        private final int[] colors;

        private FixedProvider(String id, String displayName, int priority, int[] colors) {
            this.id = id;
            this.displayName = displayName;
            this.priority = priority;
            this.colors = colors;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public int[] colorCodes(int[] runtimeColorCodes) {
            return colors;
        }
    }
}
