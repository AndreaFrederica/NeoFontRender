package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;

/** Shared absolute/relative placement helper for NFR ModularUI components. */
public final class NfrLayout {
    private NfrLayout() {
    }

    public static void place(IWidget child, int x, int y, int width, int height) {
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
        child.getArea().setSize(GuiAxis.X, Math.max(0, width));
        child.getArea().setSize(GuiAxis.Y, Math.max(0, height));
        child.resizer().setPosResized(true, true);
        child.resizer().setSizeResized(true, true);
        child.resizer().setMarginPaddingApplied(true);
        if (child instanceof ILayoutWidget) ((ILayoutWidget) child).layoutWidgets();
    }
}
