package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationAxis;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationRole;

/** Reusable scroll viewport that restores its initial position exactly once. */
public final class NfrScrollablePane extends ScrollWidget<NfrScrollablePane> implements ILayoutWidget {
    private final IWidget content;
    private int contentWidth;
    private int contentHeight;
    private boolean restored;

    public NfrScrollablePane(IWidget content) {
        super(new VerticalScrollData());
        this.content = content;
        navigationInfo(NavigationInfo.builder(NavigationRole.SCROLL_VIEW)
                .actions(NavigationAction.SCROLL_UP, NavigationAction.SCROLL_DOWN)
                .primaryAxis(NavigationAxis.VERTICAL)
                .focusable(false)
                .build());
        child(content);
    }

    public ScrollWidget<?> widget() {
        return this;
    }

    public void layout(int x, int y, int width, int height, int contentWidth, int contentHeight) {
        this.contentWidth = Math.max(0, contentWidth);
        this.contentHeight = Math.max(0, contentHeight);
        NfrLayout.place(this, x, y, width, height);
    }

    @Override
    public boolean layoutWidgets() {
        int width = Math.min(contentWidth, Math.max(0, getArea().w()));
        NfrLayout.place(content, 0, 0, width, contentHeight);
        getScrollArea().getScrollY().setScrollSize(contentHeight);
        return true;
    }

    public void restoreScrollOnce(int scrollY) {
        if (!restored) {
            getScrollArea().getScrollY().scrollTo(getScrollArea(), scrollY);
            restored = true;
        }
    }
}
