package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import com.cleanroommc.modularui.widget.ScrollWidget;

/** Reusable vertical settings-tab navigation with one selected route. */
public final class NfrSettingsTabs extends ParentWidget<NfrSettingsTabs> implements ILayoutWidget {
    private static final int ROW_HEIGHT = 24;
    private static final int GAP = 4;
    private final List<NfrCategoryButton> buttons = new ArrayList<>();

    public NfrSettingsTabs(List<Tab> tabs, IntConsumer scrollListener) {
        for (Tab tab : tabs) {
            NfrCategoryButton button = new NfrCategoryButton(tab.label, tab.selected);
            button.onMousePressed(mouseButton -> {
                if (getParent() instanceof ScrollWidget) {
                    ScrollWidget<?> scroll = (ScrollWidget<?>) getParent();
                    scrollListener.accept(scroll.getScrollArea().getScrollY().getScroll());
                }
                tab.action.run();
                return true;
            });
            buttons.add(button);
            child(button);
        }
    }

    public int preferredHeight() { return buttons.size() * (ROW_HEIGHT + GAP); }

    @Override public boolean layoutWidgets() {
        int y = 0;
        for (NfrCategoryButton button : buttons) {
            NfrLayout.place(button, 0, y, getArea().w(), ROW_HEIGHT);
            y += ROW_HEIGHT + GAP;
        }
        return true;
    }

    public static final class Tab {
        public final java.util.function.Supplier<String> label;
        public final boolean selected;
        public final Runnable action;
        public Tab(java.util.function.Supplier<String> label, boolean selected, Runnable action) {
            this.label = label; this.selected = selected; this.action = action;
        }
    }
}
