package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;

import java.util.function.Supplier;

/** Text button with a reusable right-side indicator. */
public final class NfrContentButton extends NfrTextButton {
    private final NfrButtonContent content;

    public NfrContentButton(Supplier<String> label, boolean centered, NfrButtonContent content) {
        super(label, centered);
        this.content = content;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        content.draw(this);
    }
}
