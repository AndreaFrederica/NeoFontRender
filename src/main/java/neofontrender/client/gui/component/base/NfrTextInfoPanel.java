package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/** Reusable read-only text panel. Route views supply the actual content. */
public final class NfrTextInfoPanel extends ParentWidget<NfrTextInfoPanel> implements ILayoutWidget {
    private final List<Line> lines;
    private boolean wrap;

    public NfrTextInfoPanel(Line... lines) {
        this.lines = Arrays.asList(lines);
    }

    public NfrTextInfoPanel(List<Line> lines) {
        this.lines = new java.util.ArrayList<>(lines);
    }

    public NfrTextInfoPanel(List<Line> lines, boolean wrap) {
        this(lines);
        this.wrap = wrap;
    }

    public int preferredHeight() {
        return preferredHeight(getArea().w());
    }

    public int preferredHeight(int width) {
        int lineHeight = lineHeight();
        int height = 4;
        for (Line line : lines) height += line.gapBefore + lineHeight * (wrap ? rows(line, width).size() : 1);
        return height;
    }

    @Override
    public boolean layoutWidgets() {
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        int lineHeight = lineHeight();
        int y = 4;
        for (Line line : lines) {
            y += line.gapBefore;
            for (String text : rows(line, getArea().w())) {
                minecraft.fontRenderer.drawString(text, 6, y, line.color);
                y += lineHeight;
            }
        }
    }

    private List<String> rows(Line line, int width) {
        net.minecraft.client.gui.FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int available = Math.max(1, width - 12);
        if (!wrap) return java.util.Collections.singletonList(font.trimStringToWidth(line.text.get(), available));
        String text = line.text.get();
        if (line.wrappedRows == null || line.wrappedWidth != available || !text.equals(line.wrappedText)
                || line.wrappedFont != font || line.wrappedFontHeight != font.FONT_HEIGHT) {
            // A long license inventory should only be laid out again when its width/text changes.
            List<String> rows = font.listFormattedStringToWidth(text, Math.max(32, available));
            line.wrappedRows = rows.isEmpty() ? java.util.Collections.singletonList("") : rows;
            line.wrappedWidth = available;
            line.wrappedText = text;
            line.wrappedFont = font;
            line.wrappedFontHeight = font.FONT_HEIGHT;
        }
        return line.wrappedRows;
    }

    private static int lineHeight() {
        return Math.max(18, Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 7);
    }

    public static Line line(String text, int color) {
        return line(() -> text, color);
    }

    public static Line line(Supplier<String> text, int color) {
        return new Line(text, color, 0);
    }

    public static Line spaced(String text, int color) {
        return spaced(() -> text, color);
    }

    public static Line spaced(Supplier<String> text, int color) {
        return new Line(text, color, 9);
    }

    public static Line line(Supplier<String> text, int color, int gapBefore) {
        return new Line(text, color, Math.max(0, gapBefore));
    }

    public static final class Line {
        private final Supplier<String> text;
        private final int color;
        private final int gapBefore;
        private List<String> wrappedRows;
        private int wrappedWidth;
        private int wrappedFontHeight;
        private String wrappedText;
        private net.minecraft.client.gui.FontRenderer wrappedFont;

        private Line(Supplier<String> text, int color, int gapBefore) {
            this.text = text;
            this.color = color;
            this.gapBefore = gapBefore;
        }
    }
}
