package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;

import java.util.Objects;

/**
 * Modern text plus an index map back to the original legacy string.
 */
public final class PreprocessedText {
    private final String rawText;
    private final String visibleText;
    private final ModernText modernText;
    private final boolean transformed;
    private final int[] rawStartByVisibleBoundary;
    private final int[] rawEndByVisibleBoundary;

    PreprocessedText(String rawText, String visibleText, ModernText modernText,
                     boolean transformed, int[] rawStartByVisibleBoundary,
                     int[] rawEndByVisibleBoundary) {
        this.rawText = Objects.requireNonNull(rawText, "rawText");
        this.visibleText = Objects.requireNonNull(visibleText, "visibleText");
        this.modernText = Objects.requireNonNull(modernText, "modernText");
        this.transformed = transformed;
        this.rawStartByVisibleBoundary = rawStartByVisibleBoundary;
        this.rawEndByVisibleBoundary = rawEndByVisibleBoundary;
    }

    public static PreprocessedText unchanged(String rawText) {
        String value = rawText == null ? "" : rawText;
        int[] identity = new int[value.length() + 1];
        for (int i = 0; i <= value.length(); i++) identity[i] = i;
        return new PreprocessedText(value, value, ModernText.of(value), false,
                identity, identity.clone());
    }

    public String rawText() {
        return rawText;
    }

    public String visibleText() {
        return visibleText;
    }

    public ModernText modernText() {
        return modernText;
    }

    public boolean transformed() {
        return transformed;
    }

    public int rawStartForVisibleBoundary(int boundary) {
        return rawStartByVisibleBoundary[clampBoundary(boundary)];
    }

    public int rawEndForVisibleBoundary(int boundary) {
        return rawEndByVisibleBoundary[clampBoundary(boundary)];
    }

    private int clampBoundary(int boundary) {
        return Math.max(0, Math.min(visibleText.length(), boundary));
    }
}
