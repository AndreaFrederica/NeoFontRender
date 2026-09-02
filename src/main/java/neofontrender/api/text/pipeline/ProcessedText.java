package neofontrender.api.text.pipeline;

import neofontrender.api.text.ModernText;

import java.util.Objects;

/** Modern text plus a boundary map back to the original source string. */
public final class ProcessedText {
    private final String rawText;
    private final String visibleText;
    private final ModernText modernText;
    private final boolean transformed;
    private final int[] rawStartByVisibleBoundary;
    private final int[] rawEndByVisibleBoundary;

    private ProcessedText(String rawText, String visibleText, ModernText modernText,
                          boolean transformed, int[] rawStarts, int[] rawEnds) {
        this.rawText = Objects.requireNonNull(rawText, "rawText");
        this.visibleText = Objects.requireNonNull(visibleText, "visibleText");
        this.modernText = Objects.requireNonNull(modernText, "modernText");
        this.transformed = transformed;
        this.rawStartByVisibleBoundary = rawStarts;
        this.rawEndByVisibleBoundary = rawEnds;
    }

    /** The unchanged representation is allocation-light and computes identity boundaries lazily. */
    public static ProcessedText unchanged(String rawText) {
        String value = rawText == null ? "" : rawText;
        return new ProcessedText(value, value, ModernText.of(value), false, null, null);
    }

    public static ProcessedText transformed(String rawText, String visibleText,
                                            ModernText modernText, int[] rawStarts, int[] rawEnds) {
        Objects.requireNonNull(rawStarts, "rawStarts");
        Objects.requireNonNull(rawEnds, "rawEnds");
        int required = Objects.requireNonNull(visibleText, "visibleText").length() + 1;
        if (rawStarts.length != required || rawEnds.length != required) {
            throw new IllegalArgumentException("Boundary maps must contain visibleLength + 1 entries");
        }
        return new ProcessedText(rawText == null ? "" : rawText, visibleText, modernText, true,
                rawStarts.clone(), rawEnds.clone());
    }

    /**
     * Re-bases a transformation of {@code input.visibleText()} onto the original source owned by
     * {@code input}. This is the standard way to compose raw middleware without losing trim and
     * wrapping boundaries established by an earlier stage.
     */
    public static ProcessedText compose(ProcessedText input, ProcessedText transformation) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(transformation, "transformation");
        if (!input.visibleText.equals(transformation.rawText)) {
            throw new IllegalArgumentException(
                    "Transformation source must equal the previous visible text");
        }
        if (!transformation.transformed) return input;
        int boundaries = transformation.visibleText.length() + 1;
        int[] starts = new int[boundaries];
        int[] ends = new int[boundaries];
        for (int index = 0; index < boundaries; index++) {
            starts[index] = input.rawStartForVisibleBoundary(
                    transformation.rawStartForVisibleBoundary(index));
            ends[index] = input.rawEndForVisibleBoundary(
                    transformation.rawEndForVisibleBoundary(index));
        }
        return transformed(input.rawText, transformation.visibleText,
                transformation.modernText, starts, ends);
    }

    public String rawText() { return rawText; }
    public String visibleText() { return visibleText; }
    public ModernText modernText() { return modernText; }
    public boolean transformed() { return transformed; }

    public int rawStartForVisibleBoundary(int boundary) {
        int index = clampBoundary(boundary);
        return rawStartByVisibleBoundary == null ? index : rawStartByVisibleBoundary[index];
    }

    public int rawEndForVisibleBoundary(int boundary) {
        int index = clampBoundary(boundary);
        return rawEndByVisibleBoundary == null ? index : rawEndByVisibleBoundary[index];
    }

    private int clampBoundary(int boundary) {
        return Math.max(0, Math.min(visibleText.length(), boundary));
    }
}
