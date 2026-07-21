package neofontrender.client.gui.pages;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.views.NfrEmojiTestView;

/** Full emoji diagnostics page. */
public final class NfrEmojiTestPage extends ParentWidget<NfrEmojiTestPage> implements ILayoutWidget {
    private final NfrEmojiTestView view = new NfrEmojiTestView();

    public NfrEmojiTestPage() {
        child(view);
    }

    @Override
    public boolean layoutWidgets() {
        NfrLayout.place(view, 0, 0, getArea().w(), getArea().h());
        return true;
    }
}
