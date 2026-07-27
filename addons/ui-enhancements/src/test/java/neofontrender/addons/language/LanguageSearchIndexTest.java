package neofontrender.addons.language;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageSearchIndexTest {
    @Test
    void searchesCodesAndDisplayNamesCaseInsensitively() {
        Map<String, String> languages = languages();

        assertEquals(Collections.singletonList("zh_cn"), filter(languages, "ZH_"));
        assertEquals(Collections.singletonList("en_us"), filter(languages, "english"));
    }

    @Test
    void favoritesComeFirstWithoutChangingGroupOrder() {
        Map<String, String> languages = languages();
        LinkedHashSet<String> favorites = new LinkedHashSet<>(Arrays.asList("ja_jp", "zh_cn"));

        assertEquals(Arrays.asList("zh_cn", "ja_jp", "en_us"),
                LanguageSearchIndex.filter(languages.keySet(), languages, favorites, ""));
    }

    private static List<String> filter(Map<String, String> languages, String query) {
        return LanguageSearchIndex.filter(languages.keySet(), languages, Collections.emptySet(), query);
    }

    private static Map<String, String> languages() {
        Map<String, String> languages = new LinkedHashMap<>();
        languages.put("en_us", "English (US)");
        languages.put("zh_cn", "Simplified Chinese");
        languages.put("ja_jp", "Japanese");
        return languages;
    }
}
