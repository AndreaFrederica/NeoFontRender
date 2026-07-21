package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;

/** Deterministic responsive grid for same-sized option controls. */
public final class NfrOptionsGrid extends ParentWidget<NfrOptionsGrid> implements ILayoutWidget {
    private final int itemWidth;
    private final int itemHeight;
    private final int gap;
    private final boolean expandItems;

    public NfrOptionsGrid(int itemWidth, int itemHeight, int gap, boolean expandItems) {
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
        this.gap = gap;
        this.expandItems = expandItems;
    }

    public NfrOptionsGrid add(IWidget widget) {
        child(widget);
        return this;
    }

    public int preferredHeight(int width) {
        int columns = columns(width);
        int rows = (getChildren().size() + columns - 1) / columns;
        return rows == 0 ? 0 : rows * itemHeight + (rows - 1) * gap;
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int columns = columns(width);
        int laidOutWidth = expandItems ? Math.max(0, (width - gap * (columns - 1)) / columns) : itemWidth;
        int index = 0;
        for (IWidget widget : getChildren()) {
            int column = index % columns;
            int row = index / columns;
            int x = column * (laidOutWidth + gap);
            NfrLayout.place(widget, x, row * (itemHeight + gap), Math.min(laidOutWidth, Math.max(0, width - x)), itemHeight);
            index++;
        }
        return true;
    }

    private int columns(int width) {
        return Math.max(1, (Math.max(0, width) + gap) / (itemWidth + gap));
    }
}
