package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

/** Immutable wrapped tooltip layout constrained to the current screen. */
final class TooltipLayout {
    final List<String> lines;
    final int titleLines;
    final int x;
    final int y;
    final int width;
    final int height;

    private TooltipLayout(List<String> lines, int titleLines, int x, int y, int width, int height) {
        this.lines = lines;
        this.titleLines = titleLines;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    static TooltipLayout calculate(
            List<String> source, int mouseX, int mouseY, int screenWidth, int screenHeight, FontRenderer font) {
        int width = measure(font, source);
        int available = Math.max(1, screenWidth - TooltipConfig.horizontalPadding * 2);
        if (TooltipConfig.maxWidth > 0) available = Math.min(available, TooltipConfig.maxWidth);
        int x = mouseX + TooltipConfig.cursorOffset;
        if (x + width + TooltipConfig.horizontalPadding > screenWidth) {
            x = mouseX - TooltipConfig.cursorOffset - width;
        }
        boolean wrap = width > available || x < TooltipConfig.horizontalPadding;
        List<String> lines = source;
        int titleLines = source.isEmpty() ? 0 : 1;
        if (wrap) {
            int wrapWidth = Math.max(1, Math.min(available,
                    Math.max(mouseX, screenWidth - mouseX) - TooltipConfig.cursorOffset));
            List<String> wrapped = new ArrayList<String>();
            for (int index = 0; index < source.size(); index++) {
                List<String> parts = font.listFormattedStringToWidth(source.get(index), wrapWidth);
                if (index == 0) titleLines = parts.size();
                wrapped.addAll(parts);
            }
            lines = wrapped;
            width = measure(font, lines);
            x = mouseX > screenWidth / 2
                    ? mouseX - TooltipConfig.cursorOffset - width : mouseX + TooltipConfig.cursorOffset;
        }
        int height = lines.isEmpty() ? 0 : font.FONT_HEIGHT + (lines.size() - 1) * TooltipConfig.lineHeight;
        if (lines.size() > titleLines) height += TooltipConfig.titleGap;
        int y = mouseY - TooltipConfig.cursorOffset;
        x = Math.max(TooltipConfig.horizontalPadding,
                Math.min(x, screenWidth - width - TooltipConfig.horizontalPadding));
        y = Math.max(TooltipConfig.verticalPadding,
                Math.min(y, screenHeight - height - TooltipConfig.verticalPadding));
        return new TooltipLayout(lines, titleLines, x, y, width, height);
    }

    private static int measure(FontRenderer font, List<String> lines) {
        int width = 0;
        for (String line : lines) width = Math.max(width, font.getStringWidth(line));
        return width;
    }
}
