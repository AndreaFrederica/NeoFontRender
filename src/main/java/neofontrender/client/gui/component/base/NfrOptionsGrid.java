package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;

import java.util.List;

/** Deterministic responsive grid with fixed-width columns and mixed-height option rows. */
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
        int rows = rows(columns);
        int height = 0;
        for (int row = 0; row < rows; row++) {
            height += rowHeight(row, columns);
        }
        return height + Math.max(0, rows - 1) * gap;
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int columns = columns(width);
        int laidOutWidth = expandItems ? Math.max(0, (width - gap * (columns - 1)) / columns) : itemWidth;
        List<IWidget> children = getChildren();
        int[] rowOffsets = rowOffsets(columns);
        int index = 0;
        for (IWidget widget : children) {
            int column = index % columns;
            int row = index / columns;
            int x = column * (laidOutWidth + gap);
            NfrLayout.place(widget, x, rowOffsets[row],
                    Math.min(laidOutWidth, Math.max(0, width - x)), preferredItemHeight(widget));
            index++;
        }
        return true;
    }

    private int[] rowOffsets(int columns) {
        int rows = rows(columns);
        int[] offsets = new int[rows];
        for (int row = 1; row < rows; row++) {
            offsets[row] = offsets[row - 1] + rowHeight(row - 1, columns) + gap;
        }
        return offsets;
    }

    private int rows(int columns) {
        return (getChildren().size() + columns - 1) / columns;
    }

    private int rowHeight(int row, int columns) {
        List<IWidget> children = getChildren();
        int start = row * columns;
        int end = Math.min(children.size(), start + columns);
        int height = itemHeight;
        for (int i = start; i < end; i++) {
            height = Math.max(height, preferredItemHeight(children.get(i)));
        }
        return height;
    }

    private int preferredItemHeight(IWidget widget) {
        return widget instanceof NfrPreferredHeight
                ? Math.max(itemHeight, ((NfrPreferredHeight) widget).preferredHeight())
                : itemHeight;
    }

    private int columns(int width) {
        return Math.max(1, (Math.max(0, width) + gap) / (itemWidth + gap));
    }
}
