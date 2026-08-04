package neofontrender.api.color;

/**
 * Supplies Minecraft's 32 legacy formatting colors.
 *
 * <p>Entries 0-15 are foreground colors and 16-31 are their shadow variants. Providers may use
 * the runtime {@code FontRenderer.colorCode} snapshot to integrate with another mod, or ignore it
 * and return a standalone palette. Returned arrays are copied and normalized by the registry.
 * Dynamic providers should call {@link TextColorPaletteRegistry#invalidate()} after their colors
 * or availability change.</p>
 */
public interface TextColorPaletteProvider {
    String id();

    String displayName();

    /** Higher values are preferred by the automatic selector. */
    default int priority() {
        return 0;
    }

    default boolean isAvailable() {
        return true;
    }

    /** Returns either 16 foreground entries or all 32 foreground/shadow entries. */
    int[] colorCodes(int[] runtimeColorCodes);
}
