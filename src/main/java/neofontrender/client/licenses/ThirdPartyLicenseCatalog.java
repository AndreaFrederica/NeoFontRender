package neofontrender.client.licenses;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

/** Each installed module supplies its own catalog; absent addons contribute no entries. */
public final class ThirdPartyLicenseCatalog {
    public static final String RESOURCE = "META-INF/neofontrender/third-party-licenses.json";

    private ThirdPartyLicenseCatalog() {}

    public static List<Entry> load(ClassLoader loader) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        Enumeration<URL> resources = loader.getResources(RESOURCE);
        while (resources.hasMoreElements()) {
            try (InputStream stream = resources.nextElement().openStream()) {
                for (Entry entry : read(stream)) {
                    entries.putIfAbsent(entry.section + "\n" + entry.name + "\n" + entry.version, entry);
                }
            }
        }
        List<Entry> result = new ArrayList<>(entries.values());
        result.sort(Comparator.comparing((Entry entry) -> entry.section).thenComparing(entry -> entry.name)
                .thenComparing(entry -> entry.version));
        return result;
    }

    static List<Entry> read(InputStream stream) throws IOException {
        try {
            Entry[] entries = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Entry[].class);
            if (entries == null) throw new IOException("Empty third-party license catalog");
            for (Entry entry : entries) {
                if (entry == null || blank(entry.section) || blank(entry.name) || entry.version == null
                        || blank(entry.license) || blank(entry.source) || blank(entry.evidence)) {
                    throw new IOException("Incomplete third-party license entry");
                }
            }
            return java.util.Arrays.asList(entries);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid third-party license catalog", exception);
        }
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    public static final class Entry {
        public String section;
        public String name;
        public String version;
        public String license;
        public String source;
        public String evidence;
    }
}
