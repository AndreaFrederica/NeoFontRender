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

    public NfrTextInfoPanel(Line... lines) {
        this.lines = Arrays.asList(lines);
    }

    public NfrTextInfoPanel(List<Line> lines) {
        this.lines = new java.util.ArrayList<>(lines);
    }

    public int preferredHeight() {
        int lineHeight = lineHeight();
        int height = 4;
        for (Line line : lines) height += line.gapBefore + lineHeight;
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
            String text = line.text.get();
            minecraft.fontRenderer.drawString(
                    minecraft.fontRenderer.trimStringToWidth(text, Math.max(1, getArea().w() - 12)),
                    6, y, line.color);
            y += lineHeight;
        }
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

        private Line(Supplier<String> text, int color, int gapBefore) {
            this.text = text;
            this.color = color;
            this.gapBefore = gapBefore;
        }
    }
}
