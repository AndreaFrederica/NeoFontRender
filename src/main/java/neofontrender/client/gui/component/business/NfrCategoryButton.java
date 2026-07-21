package neofontrender.client.gui.component.business;

import neofontrender.client.gui.component.base.NfrTextButton;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import net.minecraft.client.gui.Gui;

import java.util.function.Supplier;

/** Navigation-rail button with the shared selected-page treatment. */
public final class NfrCategoryButton extends NfrTextButton {
    private final boolean selected;

    public NfrCategoryButton(Supplier<String> label, boolean selected) {
        super(label, false);
        this.selected = selected;
    }

    @Override
    protected int textInset() {
        return 12;
    }

    @Override
    public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        if (selected) {
            Gui.drawRect(0, 0, getArea().w(), getArea().h(), 0xB0003438);
            Gui.drawRect(0, 0, 2, getArea().h(), 0xFF00DCE8);
            return;
        }
        super.drawBackground(context, widgetTheme);
    }
}
