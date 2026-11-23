package neofontrender.addons.audio;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrPreferredHeight;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Track rows in the NFR list style: click selects and starts the track. */
final class NfrMusicList extends ListWidget<IWidget, NfrMusicList> implements NfrPreferredHeight {
    private static final int HEIGHT = 180;
    private final Supplier<List<String>> entries;
    private final Predicate<String> visible;
    private final IntSupplier selected;
    private final IntSupplier playing;
    private final IntConsumer pick;

    NfrMusicList(Supplier<List<String>> entries, Predicate<String> visible,
                 IntSupplier selected, IntSupplier playing, IntConsumer pick) {
        this.entries = entries;
        this.visible = visible;
        this.selected = selected;
        this.playing = playing;
        this.pick = pick;
        scrollDirection(GuiAxis.Y);
        collapseDisabledChild();
        refresh();
    }

    @Override
    public int preferredHeight() { return HEIGHT; }

    public void refresh() {
        while (!getChildren().isEmpty()) remove(0);
        List<String> all = entries.get();
        if (all == null) all = Collections.emptyList();
        for (int i = 0; i < all.size(); i++) {
            String name = MusicNames.display(all.get(i));
            if (visible.test(name)) child(row(i, name, AudioModule.isBrokenEntry(all.get(i))));
        }
        if (isValid()) {
            getScrollData().scrollTo(getScrollArea(), 0);
            layoutWidgets();
        }
    }

    private ButtonWidget<?> row(int index, String name, boolean broken) {
        ButtonWidget<?> button = new ButtonWidget<>();
        TextWidget label = new TextWidget(IKey.dynamic(() ->
                (index == playing.getAsInt() ? ">> " : index == selected.getAsInt() ? "> " : "") + name));
        label.alignment(Alignment.CenterLeft);
        label.color(broken ? 0xFFFF4D5A : 0xFFFFFF);
        label.paddingLeft(6);
        button.child(label);
        button.onMousePressed(mouseButton -> { pick.accept(index); return true; });
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
