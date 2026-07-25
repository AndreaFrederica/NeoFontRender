package neofontrender.client.gui.font;

/** A font option shown by the settings screen. */
public final class FontEntry {
    public final String displayName;
    public final String familyName;
    public final String faceName;
    public final String path;

    public FontEntry(String displayName, String path) {
        this(displayName, displayName, path);
    }

    public FontEntry(String familyName, String faceName, String path) {
        this.familyName = familyName == null ? "" : familyName;
        this.faceName = faceName == null || faceName.isEmpty() ? this.familyName : faceName;
        this.path = path;
        String variant = this.faceName;
        if (variant.regionMatches(true, 0, this.familyName, 0, this.familyName.length())) {
            variant = variant.substring(this.familyName.length()).trim();
        }
        this.displayName = variant.isEmpty() ? this.familyName : this.familyName + " — " + variant;
    }
}
