package neofontrender.client.gui.component.business;

import neofontrender.client.gui.component.base.NfrLayout;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;

/** Business-level font selector layout: search, source/target actions, and a results list. */
public final class NfrFontSelector extends ParentWidget<NfrFontSelector> implements ILayoutWidget {
    private final IWidget searchLabel;
    private final IWidget searchField;
    private final IWidget targetButton;
    private final IWidget sourceButton;
    private final IWidget refreshButton;
    private final IWidget openFolderButton;
    private final IWidget sourceTitle;
    private final IWidget fontList;

    public NfrFontSelector(IWidget searchLabel, IWidget searchField, IWidget targetButton, IWidget sourceButton,
                           IWidget refreshButton, IWidget openFolderButton, IWidget sourceTitle, IWidget fontList) {
        this.searchLabel = searchLabel;
        this.searchField = searchField;
        this.targetButton = targetButton;
        this.sourceButton = sourceButton;
        this.refreshButton = refreshButton;
        this.openFolderButton = openFolderButton;
        this.sourceTitle = sourceTitle;
        this.fontList = fontList;
        child(searchLabel); child(searchField); child(targetButton); child(sourceButton);
        child(refreshButton); child(openFolderButton); child(sourceTitle); child(fontList);
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int y = 0;
        NfrLayout.place(searchLabel, 0, y, width, 12); y += 16;
        NfrLayout.place(searchField, 0, y, width, 22); y += 28;
        NfrLayout.place(targetButton, 0, y, width, 22); y += 28;
        int gap = 6;
        int available = Math.max(0, width - gap * 2);
        int first = available / 3;
        int second = available / 3;
        NfrLayout.place(sourceButton, 0, y, first, 22);
        NfrLayout.place(refreshButton, first + gap, y, second, 22);
        NfrLayout.place(openFolderButton, first + second + gap * 2, y, Math.max(0, available - first - second), 22);
        y += 32;
        NfrLayout.place(sourceTitle, 0, y, width, 12); y += 18;
        NfrLayout.place(fontList, 0, y, width, Math.max(30, getArea().h() - y));
        return true;
    }
}
