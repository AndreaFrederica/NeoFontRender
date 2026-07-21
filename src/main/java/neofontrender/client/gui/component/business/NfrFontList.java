package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.font.FontEntry;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Filterable/selectable font result list independent from the settings draft implementation. */
public final class NfrFontList extends ListWidget<IWidget, NfrFontList> {
    private final Supplier<List<FontEntry>> source;
    private final Predicate<FontEntry> visible;
    private final Predicate<FontEntry> selected;
    private final Consumer<FontEntry> select;
    private final Runnable afterSelect;
    private List<FontEntry> fonts = Collections.emptyList();

    public NfrFontList(Supplier<List<FontEntry>> source, Predicate<FontEntry> visible,
                       Predicate<FontEntry> selected, Consumer<FontEntry> select, Runnable afterSelect) {
        this.source = source;
        this.visible = visible;
        this.selected = selected;
        this.select = select;
        this.afterSelect = afterSelect;
        scrollDirection(GuiAxis.Y);
        collapseDisabledChild();
        reloadFonts();
    }

    public void reloadFonts() {
        List<FontEntry> loaded = source.get();
        fonts = loaded == null ? Collections.emptyList() : loaded;
        refresh();
    }

    public void refresh() {
        while (!getChildren().isEmpty()) remove(0);
        for (FontEntry font : fonts) {
            if (visible.test(font)) child(fontButton(font));
        }
        if (isValid()) {
            getScrollData().scrollTo(getScrollArea(), 0);
            layoutWidgets();
        }
    }

    private ButtonWidget<?> fontButton(FontEntry font) {
        ButtonWidget<?> button = new ButtonWidget<>();
        TextWidget label = new TextWidget(IKey.dynamic(
                () -> (selected.test(font) ? "> " : "") + font.displayName));
        label.alignment(Alignment.CenterLeft);
        label.color(0xFFFFFF);
        label.paddingLeft(6);
        button.child(label);
        button.onMousePressed(mouseButton -> {
            select.accept(font);
            afterSelect.run();
            return true;
        });
        button.height(16);
        return button;
    }

    @Override
    public boolean layoutWidgets() {
        int y = getArea().getPadding().getTop();
        int width = Math.max(0, getArea().w() - getArea().getPadding().horizontal());
        for (Object object : getChildren()) {
            if (!(object instanceof IWidget)) continue;
            IWidget child = (IWidget) object;
            NfrLayout.place(child, getArea().getPadding().getLeft(), y, width, 16);
            if (!child.getChildren().isEmpty()) NfrLayout.place(child.getChildren().get(0), 0, 0, width, 16);
            y += 16;
        }
        getScrollData().setScrollSize(y + getArea().getPadding().getBottom());
        return true;
    }
}
