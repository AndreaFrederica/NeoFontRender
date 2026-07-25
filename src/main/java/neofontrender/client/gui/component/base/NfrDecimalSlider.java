package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widgets.SliderWidget;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

/**
 * Compact decimal slider using the same panel, hover and typography treatment as NFR dropdowns.
 */
public final class NfrDecimalSlider extends SliderWidget {
    private static final Rectangle TRACK = new Rectangle().color(0xFF475569);
    private static final Rectangle FILL = new Rectangle().color(0xFF00AEB8);
    private static final Rectangle HANDLE = new Rectangle().color(0xFFE6ECF3);
    private final Supplier<String> label;
    private final Supplier<String> displayValue;

    public NfrDecimalSlider(Supplier<String> label, Supplier<String> displayValue) {
        this.label = label;
        this.displayValue = displayValue;
        background(new Rectangle().color(0xB0000000));
        hoverBackground(new Rectangle().color(0xB8333333));
    }

    @Override
    public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.drawBackground(context, theme);
        int width = Math.max(0, getArea().w() - 8);
        int y = Math.max(0, getArea().h() - 5);
        TRACK.draw(context, 4, y, width, 2, theme.getTheme());
        double range = getMax() - getMin();
        double normalized = range <= 0.0D ? 0.0D
                : Math.max(0.0D, Math.min(1.0D, (getSliderValue() - getMin()) / range));
        FILL.draw(context, 4, y, (int) Math.round(width * normalized), 2, theme.getTheme());
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        int pos = Math.max(4, Math.min(getArea().w() - 4, valueToPos(getSliderValue())));
        HANDLE.draw(context, pos - 2, Math.max(0, getArea().h() - 8), 5, 8, theme.getTheme());
        Platform.setupDrawFont();
        Minecraft mc = Minecraft.getMinecraft();
        int y = Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2 - 2);
        String left = mc.fontRenderer.trimStringToWidth(label.get(), Math.max(1, getArea().w() / 2 - 8));
        String right = displayValue.get();
        mc.fontRenderer.drawString(left, 4, y, 0xFFFFFF);
        mc.fontRenderer.drawString(right,
                Math.max(4, getArea().w() - mc.fontRenderer.getStringWidth(right) - 5), y, 0xE0E0E0);
    }
}
