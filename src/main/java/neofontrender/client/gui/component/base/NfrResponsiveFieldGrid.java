package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widget.ParentWidget;

import java.util.ArrayList;
import java.util.List;

/** One- or two-column responsive layout for labeled settings fields. */
public final class NfrResponsiveFieldGrid extends ParentWidget<NfrResponsiveFieldGrid> implements ILayoutWidget {
    private static final int GAP = 10;
    private static final int ROW_HEIGHT = 42;
    private final List<NfrLabeledTextField> fields = new ArrayList<>();

    public NfrResponsiveFieldGrid add(NfrLabeledTextField field) {
        fields.add(field);
        child(field);
        return this;
    }

    public int preferredHeight(int width) {
        int columns = width >= 520 ? 2 : 1;
        int rows = (fields.size() + columns - 1) / columns;
        return rows * ROW_HEIGHT + Math.max(0, rows - 1) * GAP;
    }

    @Override
    public boolean layoutWidgets() {
        int columns = getArea().w() >= 520 ? 2 : 1;
        int itemWidth = Math.max(0, (getArea().w() - GAP * (columns - 1)) / columns);
        for (int i = 0; i < fields.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            NfrLayout.place(fields.get(i), column * (itemWidth + GAP), row * (ROW_HEIGHT + GAP), itemWidth, ROW_HEIGHT);
        }
        return true;
    }
}
