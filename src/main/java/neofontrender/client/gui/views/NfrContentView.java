package neofontrender.client.gui.views;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrScrollablePane;

import java.util.function.IntUnaryOperator;

/** Shared scrollable vertical content slot used by route-specific settings views. */
public class NfrContentView<T extends NfrContentView<T>> extends ParentWidget<T> implements ILayoutWidget {
    private static final int GAP = 12;
    private final Section[] sections;
    private final NfrScrollablePane pane;

    protected NfrContentView(Section... sections) {
        this.sections = sections;
        ParentWidget<?> root = new ParentWidget<>();
        for (Section section : sections) root.child(section.widget);
        this.pane = new NfrScrollablePane(root);
        child(pane.widget());
    }

    @Override public boolean layoutWidgets() {
        int width = getArea().w(), height = getArea().h(), y = 0;
        for (Section section : sections) {
            int sectionHeight = section.height.applyAsInt(width);
            NfrLayout.place(section.widget, 0, y, Math.max(0, width - 6), sectionHeight);
            y += sectionHeight + GAP;
        }
        pane.layout(0, 0, width, height, width, Math.max(height, Math.max(0, y - GAP)));
        return true;
    }

    public static Section section(IWidget widget, IntUnaryOperator height) { return new Section(widget, height); }
    public static final class Section {
        private final IWidget widget; private final IntUnaryOperator height;
        private Section(IWidget widget, IntUnaryOperator height) { this.widget = widget; this.height = height; }
    }
}
