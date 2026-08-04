package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import neofontrender.client.gui.component.base.NfrLayout;

import java.util.List;

/**
 * History message list modeled after NfrFontList: rows are placed manually in
 * {@link #layoutWidgets()} so the ListWidget default padding check never fires
 * and row widths always match the pane minus padding.
 */
public final class NfrHistoryList extends ListWidget<IWidget, NfrHistoryList> {
    public void setRows(List<NfrHistoryRow> rows) {
        while (!getChildren().isEmpty()) remove(0);
        if (rows != null) {
            for (NfrHistoryRow row : rows) child(row);
        }
        if (isValid()) layoutWidgets();
    }

    @Override
    public boolean layoutWidgets() {
        int y = getArea().getPadding().getTop();
        int width = Math.max(0, getArea().w() - getArea().getPadding().horizontal());
        for (Object object : getChildren()) {
            if (!(object instanceof IWidget)) continue;
            IWidget child = (IWidget) object;
            NfrLayout.place(child, getArea().getPadding().getLeft(), y, width, NfrHistoryRow.rowHeight());
            y += NfrHistoryRow.rowHeight();
        }
        getScrollData().setScrollSize(y + getArea().getPadding().getBottom());
        return true;
    }
}
