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
        int height = 0;
        List<List<IWidget>> rows = visibleRows(columns);
        for (List<IWidget> row : rows) {
            height += rowHeight(row);
        }
        return height + Math.max(0, rows.size() - 1) * gap;
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int columns = columns(width);
        int laidOutWidth = expandItems ? Math.max(0, (width - gap * (columns - 1)) / columns) : itemWidth;
        List<List<IWidget>> rows = visibleRows(columns);
        int y = 0;
        for (List<IWidget> row : rows) {
            int rowHeight = rowHeight(row);
            if (row.size() == 1 && row.get(0) instanceof NfrFullRowWidget) {
                NfrLayout.place(row.get(0), 0, y, Math.max(0, width), rowHeight);
            } else {
                for (int column = 0; column < row.size(); column++) {
                    IWidget widget = row.get(column);
                    int x = column * (laidOutWidth + gap);
                    NfrLayout.place(widget, x, y,
                            Math.min(laidOutWidth, Math.max(0, width - x)), preferredItemHeight(widget));
                }
            }
            y += rowHeight + gap;
        }
        return true;
    }
    private List<List<IWidget>> visibleRows(int columns) {
        List<List<IWidget>> rows = new java.util.ArrayList<>();
        List<IWidget> current = new java.util.ArrayList<>();
        for (IWidget widget : visibleChildren()) {
            if (widget instanceof NfrFullRowWidget) {
                if (!current.isEmpty()) {
                    rows.add(current);
                    current = new java.util.ArrayList<>();
                }
                List<IWidget> fullRow = new java.util.ArrayList<>();
                fullRow.add(widget);
                rows.add(fullRow);
            } else {
                current.add(widget);
                if (current.size() == columns) {
                    rows.add(current);
                    current = new java.util.ArrayList<>();
                }
            }
        }
        if (!current.isEmpty()) rows.add(current);
        return rows;
    }

    private int rowHeight(List<IWidget> children) {
        int height = itemHeight;
        for (IWidget child : children) {
            height = Math.max(height, preferredItemHeight(child));
        }
        return height;
    }

    private List<IWidget> visibleChildren() {
        return getChildren();
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
