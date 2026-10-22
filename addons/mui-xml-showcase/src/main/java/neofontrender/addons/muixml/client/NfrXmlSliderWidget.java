package neofontrender.addons.muixml.client;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.SliderWidget;
import net.minecraft.client.gui.Gui;

/** Minimal NFR-style slider track; value and interaction remain native MUI behavior. */
public final class NfrXmlSliderWidget extends SliderWidget {
    @Override
    public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.drawBackground(context, widgetTheme);
        int y = Math.max(0, getArea().h() / 2 - 1);
        Gui.drawRect(3, y, Math.max(3, getArea().w() - 3), y + 2, 0xFF44515F);
    }
}
