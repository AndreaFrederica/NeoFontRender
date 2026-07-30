package neofontrender.addons.language;

public interface LanguageListSearchAccess {
    void nfrUi$setLanguageSearch(String query);

    boolean nfrUi$toggleFavorite(int slotIndex);
}
