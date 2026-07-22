package neofontrender.addons.hud.api;

/** Immutable, renderer-independent status sample supplied once per HUD frame. */
public final class HudBarValue {
    /** Primary value, clamped to the inclusive range from zero to {@link #maximum}. */
    public final float current;
    /** Positive finite scale shared by every layer of this sample. */
    public final float maximum;
    /** Optional secondary fill, such as saturation. */
    public final float secondary;
    /** Optional additive preview fill, such as held-food restoration. */
    public final float preview;
    /** Optional depletion indicator, such as food exhaustion. */
    public final float depletion;
    /** ARGB color of the primary fill. */
    public final int primaryColor;
    /** ARGB color of the secondary fill. */
    public final int secondaryColor;
    /** ARGB color of the preview fill. */
    public final int previewColor;
    /** ARGB color of the depletion indicator. */
    public final int depletionColor;
    /** Optional centered display text; never null. */
    public final String text;

    /** Creates a sample containing only a primary value. */
    public HudBarValue(float current, float maximum, int primaryColor, String text) {
        this(current, maximum, 0.0F, 0.0F, 0.0F,
                primaryColor, primaryColor, primaryColor, primaryColor, text);
    }

    /** Creates a sample with secondary and preview layers. */
    public HudBarValue(float current, float maximum, float secondary, float preview,
                       int primaryColor, int secondaryColor, int previewColor, String text) {
        this(current, maximum, secondary, preview, 0.0F,
                primaryColor, secondaryColor, previewColor, previewColor, text);
    }

    /** Creates a complete sample and normalizes every dynamic layer to the shared scale. */
    public HudBarValue(float current, float maximum, float secondary, float preview, float depletion,
                       int primaryColor, int secondaryColor, int previewColor, int depletionColor, String text) {
        if (Float.isNaN(maximum) || Float.isInfinite(maximum) || maximum <= 0.0F) {
            throw new IllegalArgumentException("maximum must be positive and finite");
        }
        this.maximum = maximum;
        this.current = clamp(current, maximum);
        this.secondary = clamp(secondary, maximum);
        this.preview = clamp(preview, maximum);
        this.depletion = clamp(depletion, maximum);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.previewColor = previewColor;
        this.depletionColor = depletionColor;
        this.text = text == null ? "" : text;
    }

    private static float clamp(float value, float maximum) {
        if (Float.isNaN(value)) return 0.0F;
        return Math.max(0.0F, Math.min(maximum, value));
    }
}
