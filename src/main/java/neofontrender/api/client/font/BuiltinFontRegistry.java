package neofontrender.api.client.font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Registry of namespaced font resources exposed by NFR and dependent mods. */
public final class BuiltinFontRegistry {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private BuiltinFontRegistry() {}

    /**
     * Registers a selectable font bundled in a mod resource pack.
     *
     * @param id stable namespaced catalog id, for example {@code example_mod:my_font}
     * @param familyName internal family name shown and written to the font form
     * @param location namespaced TTF/OTF/TTC resource location
     * @param defaultFallback whether NFR should append this font whenever built-in fallbacks are enabled
     */
    public static synchronized void register(String id, String familyName, String location,
                                             boolean defaultFallback) {
        Entry entry = new Entry(id, familyName, location, defaultFallback);
        Entry previous = ENTRIES.get(entry.id);
        if (previous == null) {
            ENTRIES.put(entry.id, entry);
            return;
        }
        if (!previous.equals(entry)) {
            throw new IllegalStateException("Built-in font id already registered: " + entry.id);
        }
    }

    public static synchronized List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(ENTRIES.values()));
    }

    public static final class Entry {
        private final String id;
        private final String familyName;
        private final String location;
        private final boolean defaultFallback;

        private Entry(String id, String familyName, String location, boolean defaultFallback) {
            this.id = requireResourceId(id, "id");
            this.familyName = Objects.requireNonNull(familyName, "familyName").trim();
            this.location = requireResourceId(location, "location");
            if (this.familyName.isEmpty()) throw new IllegalArgumentException("familyName is empty");
            String lower = this.location.toLowerCase(java.util.Locale.ROOT);
            if (!lower.endsWith(".ttf") && !lower.endsWith(".otf") && !lower.endsWith(".ttc")) {
                throw new IllegalArgumentException("Built-in font location is not a font: " + location);
            }
            this.defaultFallback = defaultFallback;
        }

        public String id() { return id; }
        public String namespace() { return id.substring(0, id.indexOf(':')); }
        public String familyName() { return familyName; }
        public String location() { return location; }
        public boolean defaultFallback() { return defaultFallback; }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Entry)) return false;
            Entry entry = (Entry) other;
            return defaultFallback == entry.defaultFallback && id.equals(entry.id)
                    && familyName.equals(entry.familyName) && location.equals(entry.location);
        }

        @Override public int hashCode() {
            return Objects.hash(id, familyName, location, defaultFallback);
        }

        private static String requireResourceId(String value, String label) {
            String normalized = Objects.requireNonNull(value, label).trim();
            if (!ID.matcher(normalized).matches() || normalized.contains("..")) {
                throw new IllegalArgumentException("Invalid namespaced " + label + ": " + value);
            }
            return normalized;
        }
    }
}
