package neofontrender.client.gui.layouts;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrScrollablePane;
import neofontrender.client.gui.component.business.NfrSettingsFooter;

import java.util.function.IntSupplier;

/** Application-shell layout shared by every settings route: title, tabs, view slot, and footer. */
public final class NfrSettingsLayout extends ParentWidget<NfrSettingsLayout> implements ILayoutWidget {
    private static final int PAD = 12, GAP = 12;
    private final IWidget title, view;
    private final NfrScrollablePane tabs;
    private final NfrSettingsFooter footer;
    private final IntSupplier tabsHeight;
    private final int initialTabsScroll;

    public NfrSettingsLayout(IWidget title, NfrScrollablePane tabs, IWidget view, NfrSettingsFooter footer,
                             IntSupplier tabsHeight, int initialTabsScroll) {
        this.title = title; this.tabs = tabs; this.view = view; this.footer = footer;
        this.tabsHeight = tabsHeight; this.initialTabsScroll = initialTabsScroll;
        child(title); child(tabs.widget()); child(view); child(footer);
    }

    @Override public boolean layoutWidgets() {
        int width=getArea().w(), height=getArea().h(), contentTop=PAD+28, footerHeight=footer.preferredHeight();
        int contentHeight=Math.max(0,height-contentTop-PAD-footerHeight-GAP);
        NfrLayout.place(title,PAD,PAD,Math.max(0,width-PAD*2),16);
        int tabsWidth=clamp(width/8,104,150), viewX=PAD+tabsWidth+GAP;
        tabs.layout(PAD,contentTop,tabsWidth,contentHeight,tabsWidth,tabsHeight.getAsInt());
        tabs.restoreScrollOnce(initialTabsScroll);
        NfrLayout.place(view,viewX,contentTop,Math.max(0,width-viewX-PAD),contentHeight);
        NfrLayout.place(footer,PAD,height-PAD-footerHeight,Math.max(0,width-PAD*2),footerHeight);
        return true;
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}
