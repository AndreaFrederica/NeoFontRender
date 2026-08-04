package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.sizer.StandardResizer;
import com.cleanroommc.modularui.widget.sizer.Unit;

/** Shared absolute/relative placement helper for NFR ModularUI components. */
public final class NfrLayout {
    private NfrLayout() {
    }

    public static void place(IWidget child, int x, int y, int width, int height) {
        width = Math.max(0, width);
        height = Math.max(0, height);
        // Layout widgets are resized more than once by ModularUI (for example when a scrollbar
        // activates). Persist the calculated rectangle in the resizer as well as the current
        // Area, otherwise a later partial resize falls back to the theme's default 18 x 18 size.
        StandardResizer resizer = child.resizer();
        resizer.left(x, 0, 0, Unit.Measure.PIXEL, true);
        resizer.top(y, 0, 0, Unit.Measure.PIXEL, true);
        resizer.width(width, 0, Unit.Measure.PIXEL);
        resizer.height(height, 0, Unit.Measure.PIXEL);
        int absoluteX = x;
        int absoluteY = y;
        if (child.hasParent()) {
            absoluteX += child.getParent().getArea().x();
            absoluteY += child.getParent().getArea().y();
        }
        child.getArea().setRelativePoint(GuiAxis.X, x);
        child.getArea().setRelativePoint(GuiAxis.Y, y);
        child.getArea().setPoint(GuiAxis.X, absoluteX);
        child.getArea().setPoint(GuiAxis.Y, absoluteY);
        child.getArea().setSize(GuiAxis.X, width);
        child.getArea().setSize(GuiAxis.Y, height);
        resizer.setPosResized(true, true);
        resizer.setSizeResized(true, true);
        resizer.setMarginPaddingApplied(true);
        if (child instanceof ILayoutWidget) ((ILayoutWidget) child).layoutWidgets();
    }
}
