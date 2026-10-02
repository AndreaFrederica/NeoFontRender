package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

/** Full-row settings card with a title, summary and responsive actions. */
public final class NfrSettingsCard extends ParentWidget<NfrSettingsCard>
        implements ILayoutWidget, NfrPreferredHeight, NfrFullRowWidget {
    private final Supplier<String> title;
    private final Supplier<String> summary;
    private final NfrOptionsGrid actions = new NfrOptionsGrid(180, 24, 6, true);

    public NfrSettingsCard(Supplier<String> title, Supplier<String> summary) {
        this.title = title;
        this.summary = summary;
        child(actions);
    }

    public NfrSettingsCard action(IWidget widget) { actions.add(widget); return this; }

    @Override public int preferredHeight() {
        // Reserve one row per action: stable on narrow grids without recursive height negotiation.
        return 46 + actions.getChildren().size() * 30;
    }

    @Override public boolean layoutWidgets() {
        NfrLayout.place(actions, 8, 40, Math.max(0, getArea().w() - 16), Math.max(0, getArea().h() - 46));
        return true;
    }

    @Override public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        new com.cleanroommc.modularui.drawable.Rectangle().color(0x50334455)
                .draw(context, 0, 0, getArea().w(), getArea().h(), theme.getTheme());
        Platform.setupDrawFont();
        var font = Minecraft.getMinecraft().fontRenderer;
        int width = Math.max(0, getArea().w() - 16);
        font.drawString(font.trimStringToWidth(title.get(), width), 8, 8, 0xFFD4E0EC);
        font.drawString(font.trimStringToWidth(summary.get(), width), 8, 24, 0xFFB5C0CC);
    }
}
