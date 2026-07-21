package neofontrender.client.gui.component.base;

import net.minecraft.client.gui.Gui;

import java.util.function.Supplier;

/** Compact on/off indicator for a settings row. */
public final class NfrToggleIndicator implements NfrButtonContent {
    private final Supplier<Boolean> selected;

    public NfrToggleIndicator(Supplier<Boolean> selected) {
        this.selected = selected;
    }

    @Override
    public void draw(NfrTextButton button) {
        int width = button.getArea().w();
        int height = button.getArea().h();
        int size = Math.min(12, Math.max(8, height - 8));
        int x = width - size - 6;
        int y = (height - size) / 2;
        int border = selected.get() ? 0xFF00DCE8 : 0xFFE0E0E0;
        Gui.drawRect(x, y, x + size, y + size, border);
        Gui.drawRect(x + 2, y + 2, x + size - 2, y + size - 2, 0xFF090909);
        if (selected.get()) Gui.drawRect(x + 4, y + 4, x + size - 4, y + size - 4, 0xFF00DCE8);
    }
}
