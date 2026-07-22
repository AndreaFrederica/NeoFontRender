package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import neofontrender.core.font.support.TooltipBoundsCompat;

import java.util.ArrayList;
import java.util.List;

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

    static TooltipLayout calculate(RenderTooltipEvent.Pre event) {
        FontRenderer font = event.getFontRenderer();
        List<String> source = event.getLines();
        int horizontalPadding = TooltipConfig.horizontalPadding;
        int verticalPadding = TooltipConfig.verticalPadding;
        int cursorOffset = TooltipConfig.cursorOffset;
        int width = measure(font, source);
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
        int titleLines = source.isEmpty() ? 0 : 1;
        if (wrap) {
            List<String> wrapped = new ArrayList<>();
            for (int i = 0; i < source.size(); i++) {
                List<String> part = font.listFormattedStringToWidth(source.get(i), Math.max(1, width));
                if (i == 0) titleLines = part.size();
                wrapped.addAll(part);
            }
            lines = wrapped;
            width = measure(font, lines);
            x = event.getX() > event.getScreenWidth() / 2
                    ? event.getX() - cursorOffset - horizontalPadding - width
                    : event.getX() + cursorOffset;
        }

        int height = lines.isEmpty() ? 0 : Math.max(1, font.FONT_HEIGHT - 1)
                + (lines.size() - 1) * TooltipConfig.lineHeight;
        if (lines.size() > titleLines) height += TooltipConfig.titleGap;
        int y = event.getY() - cursorOffset;
        y = Math.max(verticalPadding, Math.min(y, event.getScreenHeight() - height - verticalPadding));
        x = Math.max(horizontalPadding, Math.min(x, event.getScreenWidth() - width - horizontalPadding));
        return new TooltipLayout(lines, titleLines, x, y, width, height);
    }

    private static int measure(FontRenderer font, List<String> lines) {
        int width = 0;
        for (String line : lines) width = Math.max(width, TooltipBoundsCompat.measuredWidth(font, line));
        return width;
    }
}
