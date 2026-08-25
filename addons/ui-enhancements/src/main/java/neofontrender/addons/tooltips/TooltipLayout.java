package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.core.font.support.TooltipBoundsCompat;
import neofontrender.addons.cjk.CjkTypographyRenderer;

import java.util.ArrayList;
import java.util.List;

final class TooltipLayout {
    final List<String> lines;
    final List<Boolean> compactLines;
    final int titleLines;
    final int x;
    final int y;
    final int width;
    final int height;
    private final TooltipConfig.Profile profile;

    private TooltipLayout(List<String> lines, List<Boolean> compactLines, int titleLines,
                          int x, int y, int width, int height, TooltipConfig.Profile profile) {
        this.lines = lines;
        this.compactLines = compactLines;
        this.titleLines = titleLines;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.profile = profile;
    }

    TooltipConfig.Profile profile() { return profile; }

    float baselineOffsetBefore(int lineIndex) {
        int offset = 0;
        int count = Math.min(Math.max(0, lineIndex), lines.size());
        for (int i = 0; i < count; i++) {
            offset += Math.max(1, Math.round(advanceFor(i) * profile.textScale));
        }
        return offset;
    }

    static TooltipLayout calculate(RenderTooltipEvent.Pre event, boolean[] compactSource,
                                   TooltipConfig.Profile profile) {
        FontRenderer font = event.getFontRenderer();
        List<String> source = event.getLines();
        TooltipConfig.Profile activeProfile = profile == null ? TooltipConfig.profile("vanilla") : profile;
        int horizontalPadding = TooltipConfig.horizontalPadding;
        int verticalPadding = TooltipConfig.verticalPadding;
        int cursorOffset = TooltipConfig.cursorOffset;
        int width = measure(font, source, compactSource, activeProfile.textScale);
        int x = event.getX() + cursorOffset;
        boolean wrap = false;

        if (x + width + horizontalPadding > event.getScreenWidth()) {
            x = event.getX() - cursorOffset - horizontalPadding - width;
            if (x < horizontalPadding) {
                width = event.getX() > event.getScreenWidth() / 2
                        ? event.getX() - cursorOffset - horizontalPadding * 2
                        : event.getScreenWidth() - event.getX() - cursorOffset - horizontalPadding;
                wrap = true;
            }
        }
        if (event.getMaxWidth() > 0 && width > event.getMaxWidth()) {
            width = event.getMaxWidth();
            wrap = true;
        }
        if (TooltipConfig.maxWidth > 0 && width > TooltipConfig.maxWidth) {
            width = TooltipConfig.maxWidth;
            wrap = true;
        }

        List<String> lines = source;
        List<Boolean> compactLines = flagsFor(source.size(), compactSource);
        int titleLines = source.isEmpty() ? 0 : 1;
        if (wrap) {
            List<String> wrapped = new ArrayList<>();
            List<Boolean> wrappedCompact = new ArrayList<>();
            for (int i = 0; i < source.size(); i++) {
                int sourceWidth = compactLines.get(i) ? Math.max(1, Math.round(width * 2.0F / activeProfile.textScale))
                        : Math.max(1, Math.round(width / activeProfile.textScale));
                List<String> part = font.listFormattedStringToWidth(source.get(i), sourceWidth);
                if (i == 0) titleLines = part.size();
                wrapped.addAll(part);
                for (int j = 0; j < part.size(); j++) {
                    wrappedCompact.add(compactLines.get(i));
                }
            }
            lines = wrapped;
            compactLines = wrappedCompact;
            width = measure(font, lines, compactLines, activeProfile.textScale);
            x = event.getX() > event.getScreenWidth() / 2
                    ? event.getX() - cursorOffset - horizontalPadding - width
                    : event.getX() + cursorOffset;
        }

        int height = 0;
        for (int i = 0; i < lines.size(); i++) {
            int advance = compactLines.get(i) ? Math.max(1, Math.round(
                    ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT * activeProfile.textScale))
                    : Math.max(1, Math.round((i == 0 ? Math.max(1, font.FONT_HEIGHT - 1)
                    : TooltipConfig.lineHeight) * activeProfile.textScale));
            height += advance;
        }
        if (lines.size() > titleLines) height += TooltipConfig.titleGap;
        int y = event.getY() - cursorOffset;
        y = Math.max(verticalPadding, Math.min(y, event.getScreenHeight() - height - verticalPadding));
        x = Math.max(horizontalPadding, Math.min(x, event.getScreenWidth() - width - horizontalPadding));
        return new TooltipLayout(lines, compactLines, titleLines, x, y, width, height,
                activeProfile);
    }

    private int advanceFor(int index) {
        if (compactLines.get(index)) return ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT;
        // Forge's original divider uses the configured baseline spacing for every title line.
        // The first-line font height belongs to panel sizing, not to the divider coordinate.
        return TooltipConfig.lineHeight;
    }

    private static List<Boolean> flagsFor(int size, boolean[] compactSource) {
        List<Boolean> flags = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            flags.add(compactSource != null && i < compactSource.length && compactSource[i]);
        }
        return flags;
    }

    private static int measure(FontRenderer font, List<String> lines, boolean[] compactSource,
                               float textScale) {
        int width = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineWidth = Math.max(TooltipBoundsCompat.measuredWidth(font, line),
                    CjkTypographyRenderer.measuredWidth(font, line));
            if (compactSource != null && i < compactSource.length && compactSource[i]) {
                lineWidth = (lineWidth + 1) / 2;
            }
            width = Math.max(width, Math.max(1, Math.round(lineWidth * textScale)));
        }
        return width;
    }

    private static int measure(FontRenderer font, List<String> lines, List<Boolean> compactLines,
                               float textScale) {
        boolean[] compact = new boolean[compactLines.size()];
        for (int i = 0; i < compact.length; i++) compact[i] = compactLines.get(i);
        return measure(font, lines, compact, textScale);
    }
}
