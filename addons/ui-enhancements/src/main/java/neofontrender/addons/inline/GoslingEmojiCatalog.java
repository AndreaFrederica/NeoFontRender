package neofontrender.addons.inline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neofontrender.addons.ui.NfrUiEnhancements;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Compact runtime index built from the MIT-licensed Gosling/Twemoji dictionary. */
final class GoslingEmojiCatalog {
    private static final String RESOURCE =
            "/assets/neofontrender_ui_enhancements/emoji/gosling-emojis.min.json";
    static final GoslingEmojiCatalog INSTANCE = load();

    private final Map<String, Entry> aliases;
    private final List<String> aliasNames;

    private GoslingEmojiCatalog(Map<String, Entry> aliases) {
        this.aliases = aliases;
        List<String> names = new ArrayList<>(aliases.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        this.aliasNames = Collections.unmodifiableList(names);
    }

    @Nullable Entry alias(String value) {
        return aliases.get(value.toLowerCase(Locale.ROOT));
    }

    List<String> suggestions(String prefix, int maximum) {
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String alias : aliasNames) {
            if (!alias.startsWith(needle)) continue;
            result.add(":" + alias + ":");
            if (result.size() >= maximum) break;
        }
        return result;
    }

    private static GoslingEmojiCatalog load() {
        Map<String, Entry> aliases = new HashMap<>();
        try (InputStream stream = GoslingEmojiCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing " + RESOURCE);
            JsonObject root = new JsonParser().parse(new InputStreamReader(stream,
                    StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement groupElement : root.getAsJsonArray("groups")) {
                JsonObject group = groupElement.getAsJsonObject();
                String base = group.get("location").getAsString();
                for (JsonElement emojiElement : group.getAsJsonArray("emojis")) {
                    JsonObject emoji = emojiElement.getAsJsonObject();
                    String name = emoji.get("name").getAsString();
                    URI uri = URI.create(base + emoji.get("location").getAsString());
                    Entry entry = new Entry(name, uri);
                    JsonArray strings = emoji.getAsJsonArray("strings");
                    if (strings != null) for (JsonElement alias : strings) {
                        aliases.putIfAbsent(alias.getAsString().toLowerCase(Locale.ROOT), entry);
                    }
                    aliases.putIfAbsent(name.toLowerCase(Locale.ROOT), entry);
                }
            }
            return new GoslingEmojiCatalog(Collections.unmodifiableMap(aliases));
        } catch (Throwable failure) {
            NfrUiEnhancements.LOGGER.error("Could not load the bundled Gosling emoji dictionary", failure);
            return new GoslingEmojiCatalog(Collections.emptyMap());
        }
    }

    static final class Entry {
        final String name;
        final URI uri;

        private Entry(String name, URI uri) {
            this.name = name;
            this.uri = uri;
        }
    }
}
