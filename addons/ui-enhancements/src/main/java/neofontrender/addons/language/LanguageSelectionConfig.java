package neofontrender.addons.language;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LanguageSelectionConfig {
    private static final Set<String> FAVORITES = new LinkedHashSet<>();
    private static boolean loaded;

    private LanguageSelectionConfig() {}

    public static synchronized boolean isFavorite(String languageCode) {
        load();
        return FAVORITES.contains(languageCode);
    }

    public static synchronized boolean toggleFavorite(String languageCode) {
        load();
        boolean favorite;
        if (FAVORITES.remove(languageCode)) {
            favorite = false;
        } else {
            FAVORITES.add(languageCode);
            favorite = true;
        }
        UiEnhancementsConfig.file().set("languageScreen.favorites", new ArrayList<>(FAVORITES)).save();
        return favorite;
    }

    public static synchronized Set<String> favorites() {
        load();
        return Collections.unmodifiableSet(new LinkedHashSet<>(FAVORITES));
    }

    private static void load() {
        if (loaded) return;
        NfrConfigFile file = UiEnhancementsConfig.file();
        List<String> defaults = Collections.emptyList();
        file.define("languageScreen.favorites", defaults,
                "Language codes pinned to the top of the language selection screen.");
        FAVORITES.addAll(file.getStringList("languageScreen.favorites", defaults));
        loaded = true;
        file.save();
    }
}
