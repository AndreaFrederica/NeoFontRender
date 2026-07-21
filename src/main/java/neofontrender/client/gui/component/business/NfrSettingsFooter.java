package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLayout;

/** Shared settings-page footer: auxiliary action left, Apply/Cancel grouped on the right. */
public final class NfrSettingsFooter extends ParentWidget<NfrSettingsFooter> implements ILayoutWidget {
    private static final int GAP = 12;
    private final IWidget auxiliary, apply, cancel;

    public NfrSettingsFooter(IWidget auxiliary, IWidget apply, IWidget cancel) {
        this.auxiliary = auxiliary; this.apply = apply; this.cancel = cancel;
        child(auxiliary); child(apply); child(cancel);
    }

    public int preferredHeight() { return 24; }

    @Override public boolean layoutWidgets() {
        int width = getArea().w(), buttonWidth = Math.min(160, Math.max(90, (width - GAP * 2) / 3));
        NfrLayout.place(auxiliary, 0, 0, buttonWidth, preferredHeight());
        NfrLayout.place(cancel, width - buttonWidth, 0, buttonWidth, preferredHeight());
        NfrLayout.place(apply, Math.max(0, width - buttonWidth * 2 - GAP), 0, buttonWidth, preferredHeight());
        return true;
    }
}
