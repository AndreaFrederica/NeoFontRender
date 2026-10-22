package neofontrender.api.text.paragraph;

import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Optional provider for positioned paragraph layout and component-aware wrapping. */
public interface TextParagraphProvider {
    String id();
    default int priority() { return 0; }
    default boolean isEnabled() { return true; }
    default Layout layout(Request request) { return null; }
    default List<ITextComponent> splitComponents(ComponentRequest request) { return null; }

    @FunctionalInterface
    interface TextMeasurer {
        float measureFormatted(String text);
    }

    final class Request {
        private final String formattedText;
        private final int maxWidth;
        private final int lineHeight;
        private final String languageCode;
        private final TextMeasurer measurer;

        public Request(String formattedText, int maxWidth, int lineHeight,
                       String languageCode, TextMeasurer measurer) {
            this.formattedText = Objects.requireNonNull(formattedText, "formattedText");
            this.maxWidth = Math.max(1, maxWidth);
            this.lineHeight = Math.max(1, lineHeight);
            this.languageCode = languageCode == null ? "" : languageCode;
            this.measurer = Objects.requireNonNull(measurer, "measurer");
        }

        public String formattedText() { return formattedText; }
        public int maxWidth() { return maxWidth; }
        public int lineHeight() { return lineHeight; }
        public String languageCode() { return languageCode; }
        public TextMeasurer measurer() { return measurer; }
    }

    final class ComponentRequest {
        public enum Surface { DEFAULT, CHAT, BOOK, TOOLTIP }

        private final ITextComponent component;
        private final int maxWidth;
        private final int lineHeight;
        private final String languageCode;
        private final boolean removeLeadingSpace;
        private final boolean forceTextColor;
        private final TextMeasurer measurer;
        private final Surface surface;

        public ComponentRequest(ITextComponent component, int maxWidth, int lineHeight,
                                String languageCode, boolean removeLeadingSpace,
                                boolean forceTextColor, TextMeasurer measurer) {
            this(component, maxWidth, lineHeight, languageCode, removeLeadingSpace,
                    forceTextColor, measurer, Surface.DEFAULT);
        }

        public ComponentRequest(ITextComponent component, int maxWidth, int lineHeight,
                                String languageCode, boolean removeLeadingSpace,
                                boolean forceTextColor, TextMeasurer measurer, Surface surface) {
            this.component = Objects.requireNonNull(component, "component");
            this.maxWidth = Math.max(1, maxWidth);
            this.lineHeight = Math.max(1, lineHeight);
            this.languageCode = languageCode == null ? "" : languageCode;
            this.removeLeadingSpace = removeLeadingSpace;
            this.forceTextColor = forceTextColor;
            this.measurer = Objects.requireNonNull(measurer, "measurer");
            this.surface = surface == null ? Surface.DEFAULT : surface;
        }

        public ITextComponent component() { return component; }
        public int maxWidth() { return maxWidth; }
        public int lineHeight() { return lineHeight; }
        public String languageCode() { return languageCode; }
        public boolean removeLeadingSpace() { return removeLeadingSpace; }
        public boolean forceTextColor() { return forceTextColor; }
        public TextMeasurer measurer() { return measurer; }
        public Surface surface() { return surface; }
    }

    final class Layout {
        private final List<Line> lines;

        public Layout(List<Line> lines) {
            this.lines = Collections.unmodifiableList(new ArrayList<>(
                    Objects.requireNonNull(lines, "lines")));
        }

        public List<Line> lines() { return lines; }
        public int firstSourceBoundary(int fallback) {
            return lines.isEmpty() ? fallback : lines.get(0).sourceEnd();
        }
    }

    final class Line {
        private final int sourceStart;
        private final int sourceEnd;
        private final float yOffset;
        private final boolean hardBreak;
        private final List<Run> runs;

        public Line(int sourceStart, int sourceEnd, float yOffset,
                    boolean hardBreak, List<Run> runs) {
            if (sourceStart < 0 || sourceEnd < sourceStart) {
                throw new IllegalArgumentException("Invalid source line range");
            }
            this.sourceStart = sourceStart;
            this.sourceEnd = sourceEnd;
            this.yOffset = yOffset;
            this.hardBreak = hardBreak;
            this.runs = Collections.unmodifiableList(new ArrayList<>(
                    Objects.requireNonNull(runs, "runs")));
        }

        public int sourceStart() { return sourceStart; }
        public int sourceEnd() { return sourceEnd; }
        public float yOffset() { return yOffset; }
        public boolean hardBreak() { return hardBreak; }
        public List<Run> runs() { return runs; }
    }

    final class Run {
        private final String formattedText;
        private final float xOffset;
        private final int sourceStart;
        private final int sourceEnd;

        public Run(String formattedText, float xOffset) {
            this(formattedText, xOffset, -1, -1);
        }

        public Run(String formattedText, float xOffset, int sourceStart, int sourceEnd) {
            this.formattedText = Objects.requireNonNull(formattedText, "formattedText");
            this.xOffset = xOffset;
            this.sourceStart = sourceStart;
            this.sourceEnd = sourceEnd;
        }

        public String formattedText() { return formattedText; }
        public float xOffset() { return xOffset; }
        public int sourceStart() { return sourceStart; }
        public int sourceEnd() { return sourceEnd; }
    }
}
