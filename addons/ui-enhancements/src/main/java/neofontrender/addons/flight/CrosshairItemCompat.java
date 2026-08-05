package neofontrender.addons.flight;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exact-ID compatibility registry backed by the bundled TOML plus user config additions. */
final class CrosshairItemCompat {
    enum Kind { CROSSBOW, SPYGLASS, TRIDENT, RANGED }

    private static final String RESOURCE =
            "assets/neofontrender_ui_enhancements/crosshair_compat.toml";
    private static final Pattern QUOTED_ID = Pattern.compile(
            "\\\"([a-z0-9_.-]+:[a-z0-9_./-]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Map<Kind, Set<String>> BUNDLED = loadBundled();
    private static volatile Map<Kind, Set<String>> active = BUNDLED;

    private CrosshairItemCompat() {}

    static void configure(String spyglasses, String crossbows, String tridents, String ranged) {
        EnumMap<Kind, Set<String>> merged = mutableCopy(BUNDLED);
        addConfigured(merged.get(Kind.SPYGLASS), spyglasses);
        addConfigured(merged.get(Kind.CROSSBOW), crossbows);
        addConfigured(merged.get(Kind.TRIDENT), tridents);
        addConfigured(merged.get(Kind.RANGED), ranged);
        active = immutableCopy(merged);
    }

    static boolean matches(Kind kind, ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) return false;
        return matches(kind, stack.getItem().getRegistryName().toString());
    }

    static boolean matches(Kind kind, String id) {
        return id != null && active.get(kind).contains(normalize(id));
    }

    static Map<Kind, Set<String>> parse(InputStream stream) throws IOException {
        EnumMap<Kind, Set<String>> result = emptyMutable();
        if (stream == null) return immutableCopy(result);
        Kind current = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.substring(0, commentAt(line)).trim();
                if (trimmed.startsWith("crossbow") && trimmed.contains("=")) current = Kind.CROSSBOW;
                else if (trimmed.startsWith("spyglass") && trimmed.contains("=")) current = Kind.SPYGLASS;
                else if (trimmed.startsWith("trident") && trimmed.contains("=")) current = Kind.TRIDENT;
                else if (trimmed.startsWith("ranged") && trimmed.contains("=")) current = Kind.RANGED;
                if (current == null) continue;
                Matcher matcher = QUOTED_ID.matcher(trimmed);
                while (matcher.find()) result.get(current).add(normalize(matcher.group(1)));
                if (trimmed.contains("]")) current = null;
            }
        }
        return immutableCopy(result);
    }

    private static Map<Kind, Set<String>> loadBundled() {
        try (InputStream stream = CrosshairItemCompat.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            return parse(stream);
        } catch (IOException ignored) {
            return immutableCopy(emptyMutable());
        }
    }

    private static void addConfigured(Set<String> target, String configured) {
        if (configured == null || configured.trim().isEmpty()) return;
        for (String token : configured.split("[,;\\s]+")) {
            String id = normalize(token);
            if (id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) target.add(id);
        }
    }

    private static int commentAt(String line) {
        int index = line.indexOf('#');
        return index < 0 ? line.length() : index;
    }

    private static String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static EnumMap<Kind, Set<String>> emptyMutable() {
        EnumMap<Kind, Set<String>> map = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) map.put(kind, new LinkedHashSet<>());
        return map;
    }

    private static EnumMap<Kind, Set<String>> mutableCopy(Map<Kind, Set<String>> source) {
        EnumMap<Kind, Set<String>> copy = emptyMutable();
        for (Kind kind : Kind.values()) copy.get(kind).addAll(source.get(kind));
        return copy;
    }

    private static Map<Kind, Set<String>> immutableCopy(Map<Kind, Set<String>> source) {
        EnumMap<Kind, Set<String>> copy = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            copy.put(kind, Collections.unmodifiableSet(new LinkedHashSet<>(source.get(kind))));
        }
        return Collections.unmodifiableMap(copy);
    }
}
