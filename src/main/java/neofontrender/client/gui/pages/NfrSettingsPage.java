package neofontrender.client.gui.pages;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.layouts.NfrSettingsLayout;

/** Route-level settings page. The layout owns chrome; the selected view owns route content. */
public final class NfrSettingsPage extends ParentWidget<NfrSettingsPage> implements ILayoutWidget {
    private final NfrSettingsLayout layout;

    public NfrSettingsPage(NfrSettingsLayout layout) {
        this.layout = layout;
        child(layout);
    }

    @Override public boolean layoutWidgets() {
        NfrLayout.place(layout, 0, 0, getArea().w(), getArea().h());
        return true;
    }
}
