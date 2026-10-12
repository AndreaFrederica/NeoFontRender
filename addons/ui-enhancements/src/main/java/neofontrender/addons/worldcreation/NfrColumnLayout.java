package neofontrender.addons.worldcreation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal vertical column layouter for vanilla {@code GuiScreen}-based screens.
 *
 * <p>Rows are fixed-height items separated by flexible gaps. When the available band is
 * shorter than the preferred total, gaps shrink toward their minimum (proportionally) so
 * rows never overlap; when it is taller, gaps get their preferred size and the column
 * stays top-aligned. This keeps the create-world layout stable across window resizes
 * instead of relying on hardcoded y coordinates.
 */
public final class NfrColumnLayout {
    private static final class Element {
        final String key; // null for gaps
        final int preferred; // item height, or preferred gap size
        final int min; // minimum gap size (items: same as preferred)

        Element(String key, int preferred, int min) {
            this.key = key;
            this.preferred = preferred;
            this.min = min;
        }
    }

    private final List<Element> elements = new ArrayList<>();

    /** Adds a fixed-height row retrievable by key from {@link #layout(int, int)}. */
    public NfrColumnLayout item(String key, int height) {
        elements.add(new Element(key, height, height));
        return this;
    }

    /** Adds flexible space before the next row; shrinks toward {@code min} when space is tight. */
    public NfrColumnLayout gap(int preferred, int min) {
        if (min > preferred) throw new IllegalArgumentException("min > preferred");
        elements.add(new Element(null, preferred, min));
        return this;
    }

    /**
     * Computes the y position of every row for the content band {@code [top, bottom]}.
     * Rows never overlap; gaps absorb both leftover space and (up to their minimum) shortage.
     *
     * @return row key -&gt; top y coordinate
     */
    public Map<String, Integer> layout(int top, int bottom) {
        int fixed = 0;
        int preferredGaps = 0;
        int minGaps = 0;
        int weights = 0;
        for (Element element : elements) {
            if (element.key == null) {
                preferredGaps += element.preferred;
                minGaps += element.min;
                weights += element.preferred - element.min;
            } else {
                fixed += element.preferred;
            }
        }

        int budget = Math.max(0, (bottom - top) - fixed);
        Map<String, Integer> positions = new HashMap<>();
        int y = top;
        int remainingWeight = weights;
        int remainingExtra = Math.max(0, budget - minGaps);

        for (Element element : elements) {
            if (element.key == null) {
                int size;
                if (budget >= preferredGaps) {
                    size = element.preferred;
                } else if (remainingWeight <= 0) {
                    size = element.min;
                } else {
                    int share = remainingExtra * (element.preferred - element.min) / remainingWeight;
                    size = element.min + share;
                    remainingExtra -= share;
                    remainingWeight -= element.preferred - element.min;
                }
                y += size;
            } else {
                positions.put(element.key, y);
                y += element.preferred;
            }
        }
        return positions;
    }
}
