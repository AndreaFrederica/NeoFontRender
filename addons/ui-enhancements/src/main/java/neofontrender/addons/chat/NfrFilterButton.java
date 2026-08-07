package neofontrender.addons.chat;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import net.minecraft.client.gui.Gui;
import neofontrender.client.gui.component.base.NfrTextButton;

import java.util.function.Supplier;

/** NFR-styled filter chip with a highlighted selected state. */
public final class NfrFilterButton extends NfrTextButton {
    private final Supplier<Boolean> selected;

    public NfrFilterButton(Supplier<String> label, boolean centered, Supplier<Boolean> selected) {
        super(label, centered);
        this.selected = selected;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        // Selection/hover accents drawn after the themed background so they stay visible.
        if (selected.get()) {
            Gui.drawRect(1, getArea().h() - 2, getArea().w() - 1, getArea().h(), 0xFF336CFF);
        } else if (context.isHovered(this)) {
            Gui.drawRect(1, getArea().h() - 2, getArea().w() - 1, getArea().h(), 0x80FFFFFF);
        }
    }
}
