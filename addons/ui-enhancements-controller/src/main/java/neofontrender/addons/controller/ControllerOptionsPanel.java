package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrOptionsGrid;

/** Titled settings group used to keep controller modes visually separate. */
final class ControllerOptionsPanel extends ParentWidget<ControllerOptionsPanel>
        implements ILayoutWidget {
    private static final int HEADER_HEIGHT = 28;
    private final String title;
    private final NfrOptionsGrid grid;

    ControllerOptionsPanel(String title, NfrOptionsGrid grid) {
        this.title = title;
        this.grid = grid;
        child(grid);
    }

    int preferredHeight(int width) {
        return HEADER_HEIGHT + grid.preferredHeight(width);
    }

    @Override
    public boolean layoutWidgets() {
        NfrLayout.place(grid, 0, HEADER_HEIGHT, getArea().w(),
                grid.preferredHeight(getArea().w()));
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        ControllerArc3D.draw(this::drawHeader);
    }

    private void drawHeader(FlightHudCanvas canvas) {
        canvas.text(title, 0, 5, 0.78F, 0xFFF1F5F9, 0xD0000000);
        canvas.line(0, 23, getArea().w(), 23, 0x556A7787, 1.0F);
    }
}
