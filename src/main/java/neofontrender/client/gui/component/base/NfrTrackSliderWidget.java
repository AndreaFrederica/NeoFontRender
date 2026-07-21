package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.SliderWidget;

/** SliderWidget variant that draws its missing track before the stopper marks. */
public final class NfrTrackSliderWidget extends SliderWidget {
    private final Rectangle track = new Rectangle().color(0xFF475569).cornerRadius(2);

    @Override
    public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int width = Math.max(0, getArea().w() - 8);
        track.draw(context, 4, getArea().h() / 2 - 2, width, 4, widgetTheme.getTheme());
        super.drawBackground(context, widgetTheme);
    }
}
