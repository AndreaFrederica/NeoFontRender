package neofontrender.addons.hud.api;

/** Immutable, renderer-independent status-bar sample supplied once per HUD frame. */
public final class HudBarValue {
    public final float current;
    public final float maximum;
    public final float secondary;
    public final float preview;
    public final float secondaryPreview;
    public final float depletion;
    public final int primaryColor;
    public final int secondaryColor;
    public final int previewColor;
    public final int depletionColor;
    public final String text;
    public final boolean animatePreview;

    public HudBarValue(float current, float maximum, int primaryColor, String text) {
        this(current, maximum, 0.0F, 0.0F, 0.0F,
                primaryColor, primaryColor, primaryColor, primaryColor, text);
    }

    public HudBarValue(float current, float maximum, float secondary, float preview,
                       int primaryColor, int secondaryColor, int previewColor, String text) {
        this(current, maximum, secondary, preview, 0.0F,
                primaryColor, secondaryColor, previewColor, previewColor, text);
    }

    public HudBarValue(float current, float maximum, float secondary, float preview, float depletion,
                       int primaryColor, int secondaryColor, int previewColor, int depletionColor, String text) {
        this(current, maximum, secondary, preview, 0.0F, depletion, primaryColor, secondaryColor,
                previewColor, depletionColor, text, false);
    }

    public HudBarValue(float current, float maximum, float secondary, float preview, float secondaryPreview,
                       float depletion,
                       int primaryColor, int secondaryColor, int previewColor, int depletionColor,
                       String text, boolean animatePreview) {
        // Retained from the 1.7.10 port: every dynamic layer is clamped to the shared scale and a
        // non-positive or non-finite scale is rejected, so renderers can divide by maximum safely.
        if (Float.isNaN(maximum) || Float.isInfinite(maximum) || maximum <= 0.0F) {
            throw new IllegalArgumentException("maximum must be positive and finite");
        }
        this.maximum = maximum;
        this.current = clamp(current, maximum);
        this.secondary = clamp(secondary, maximum);
        this.preview = clamp(preview, maximum);
        this.secondaryPreview = clamp(secondaryPreview, maximum);
        this.depletion = clamp(depletion, maximum);
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.previewColor = previewColor;
        this.depletionColor = depletionColor;
        this.text = text == null ? "" : text;
        this.animatePreview = animatePreview;
    }

    private static float clamp(float value, float maximum) {
        if (Float.isNaN(value)) return 0.0F;
        return Math.max(0.0F, Math.min(maximum, value));
    }
}
