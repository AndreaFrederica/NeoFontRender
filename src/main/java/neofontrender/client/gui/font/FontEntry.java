package neofontrender.client.gui.font;

/** A font option shown by the settings screen. */
public final class FontEntry {
    public final String displayName;
    public final String path;

    public FontEntry(String displayName, String path) {
        this.displayName = displayName;
        this.path = path;
    }
}
