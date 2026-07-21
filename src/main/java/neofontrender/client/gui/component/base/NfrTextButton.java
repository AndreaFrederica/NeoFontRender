package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

/** Shared text button with stable font drawing for ModularUI screens. */
public class NfrTextButton extends ButtonWidget<NfrTextButton> {
    private final Supplier<String> label;
    private final boolean centered;

    public NfrTextButton(Supplier<String> label, boolean centered) {
        this.label = label;
        this.centered = centered;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        Platform.setupDrawFont();
        Minecraft mc = Minecraft.getMinecraft();
        String text = label.get();
        int inset = textInset();
        String visible = mc.fontRenderer.trimStringToWidth(text, Math.max(1, getArea().w() - inset - 4));
        int x = centered ? Math.max(inset, (getArea().w() - mc.fontRenderer.getStringWidth(visible)) / 2) : inset;
        int y = Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2);
        mc.fontRenderer.drawString(visible, x, y, 0xFFFFFF);
    }

    protected int textInset() {
        return 4;
    }
}
