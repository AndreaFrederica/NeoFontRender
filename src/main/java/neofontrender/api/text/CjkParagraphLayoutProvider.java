package neofontrender.api.text;

import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Optional paragraph-layout extension used by addons that provide richer CJK typography.
 *
 * <p>The contract intentionally contains no implementation-specific layout types. Returning
 * {@code null} from either operation asks NeoFontRender to use its built-in lightweight CJK
 * line-break rules.</p>
 */
public interface CjkParagraphLayoutProvider {
    String id();

    default int priority() {
        return 0;
    }

    default Layout layout(Request request) {
        return null;
    }

    default List<ITextComponent> splitComponents(ComponentRequest request) {
        return null;
    }

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
        public enum Surface {
            DEFAULT,
            CHAT,
            BOOK,
            TOOLTIP
        }

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
            Objects.requireNonNull(lines, "lines");
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }

        public List<Line> lines() { return lines; }

        public int firstRawBoundary(int fallback) {
            return lines.isEmpty() ? fallback : lines.get(0).rawEnd();
        }
    }

    final class Line {
        private final int rawStart;
        private final int rawEnd;
        private final float yOffset;
        private final boolean hardBreak;
        private final List<Run> runs;

        public Line(int rawStart, int rawEnd, float yOffset,
                    boolean hardBreak, List<Run> runs) {
            if (rawStart < 0 || rawEnd < rawStart) {
                throw new IllegalArgumentException("Invalid raw line range");
            }
            this.rawStart = rawStart;
            this.rawEnd = rawEnd;
            this.yOffset = yOffset;
            this.hardBreak = hardBreak;
            this.runs = Collections.unmodifiableList(new ArrayList<>(
                    Objects.requireNonNull(runs, "runs")));
        }

        public int rawStart() { return rawStart; }
        public int rawEnd() { return rawEnd; }
        public float yOffset() { return yOffset; }
        public boolean hardBreak() { return hardBreak; }
        public List<Run> runs() { return runs; }
    }

    final class Run {
        private final String formattedText;
        private final float xOffset;
        private final int rawStart;
        private final int rawEnd;

        public Run(String formattedText, float xOffset) {
            this(formattedText, xOffset, -1, -1);
        }

        public Run(String formattedText, float xOffset, int rawStart, int rawEnd) {
            this.formattedText = Objects.requireNonNull(formattedText, "formattedText");
            this.xOffset = xOffset;
            this.rawStart = rawStart;
            this.rawEnd = rawEnd;
        }

        public String formattedText() { return formattedText; }
        public float xOffset() { return xOffset; }
        public int rawStart() { return rawStart; }
        public int rawEnd() { return rawEnd; }
    }
}
