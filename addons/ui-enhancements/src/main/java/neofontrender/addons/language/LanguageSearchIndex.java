package neofontrender.addons.language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LanguageSearchIndex {
    private LanguageSearchIndex() {}

    public static <T> List<String> filter(Collection<String> languageCodes,
                                           Map<String, T> languages,
                                           Set<String> favorites,
                                           String query) {
        String needle = normalize(query);
        List<RankedLanguage> matches = new ArrayList<>();
        int order = 0;
        for (String code : languageCodes) {
            T language = languages.get(code);
            String displayName = language == null ? "" : language.toString();
            if (needle.isEmpty() || normalize(code).contains(needle) || normalize(displayName).contains(needle)) {
                matches.add(new RankedLanguage(code, favorites.contains(code), order));
            }
            order++;
        }
        matches.sort(Comparator.comparing(RankedLanguage::favorite).reversed()
                .thenComparingInt(RankedLanguage::order));
        List<String> result = new ArrayList<>(matches.size());
        for (RankedLanguage match : matches) result.add(match.code());
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class RankedLanguage {
        private final String code;
        private final boolean favorite;
        private final int order;

        private RankedLanguage(String code, boolean favorite, int order) {
            this.code = code;
            this.favorite = favorite;
            this.order = order;
        }

        private String code() { return code; }
        private boolean favorite() { return favorite; }
        private int order() { return order; }
    }
}
