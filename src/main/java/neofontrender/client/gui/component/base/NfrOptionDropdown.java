package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Dropdown option row supporting regular label/value and compact value-only presentation. */
public final class NfrOptionDropdown extends ContextMenuButton<NfrOptionDropdown> {
    private static final int MENU_BACKGROUND = 0xF0121720;
    private static final int OPTION_BACKGROUND = 0xF018202C;
    private static final int OPTION_HOVER_BACKGROUND = 0xFF263447;
    private static final int MENU_BORDER = 0xFF64748B;

    private final Supplier<String> label;
    private final Supplier<String> getter;
    private final Function<String, String> display;
    private final boolean compact;

    public NfrOptionDropdown(String name, Supplier<String> label, Supplier<String> getter,
                             Consumer<String> setter, Iterable<String> values,
                             Function<String, String> display, boolean compact) {
        super(name);
        this.label = label;
        this.getter = getter;
        this.display = display;
        this.compact = compact;
        requiresClick();
        menu(createMenu(setter, values));
    }

    private Menu<?> createMenu(Consumer<String> setter, Iterable<String> values) {
        ListWidget<IWidget, ?> list = new ListWidget<>()
                .widthRel(1f)
                .maxSize(144)
                .background(new Rectangle().color(MENU_BACKGROUND));
        for (String value : values) {
            NfrTextButton option = new NfrTextButton(() -> display.apply(value), false)
                    .height(20)
                    .background(new Rectangle().color(OPTION_BACKGROUND))
                    .hoverBackground(new Rectangle().color(OPTION_HOVER_BACKGROUND))
                    .onMousePressed(button -> {
                        setter.accept(value);
                        closeMenu(false);
                        return true;
                    });
            option.relativeToParent().fullWidth();
            list.child(option);
        }
        return new Menu<>()
                .widthRel(1f)
                .coverChildrenHeight()
                .background(new Rectangle().color(MENU_BORDER))
                .padding(1)
                .child(list);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.draw(context, theme);
        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        int width = getArea().w();
        int height = getArea().h();
        int y = Math.max(0, (height - minecraft.fontRenderer.FONT_HEIGHT) / 2);
        String value = display.apply(getter.get());
        if (compact) drawCompactValue(minecraft, value, width, y);
        else drawLabelAndValue(minecraft, value, width, y);
        drawArrow(width, height);
    }

    private void drawCompactValue(Minecraft minecraft, String value, int width, int y) {
        String visible = minecraft.fontRenderer.trimStringToWidth(value, Math.max(1, width - 22));
        int x = Math.max(4, (width - 14 - minecraft.fontRenderer.getStringWidth(visible)) / 2);
        minecraft.fontRenderer.drawString(visible, x, y, 0xFFFFFF);
    }

    private void drawLabelAndValue(Minecraft minecraft, String value, int width, int y) {
        int inset = 4;
        String left = minecraft.fontRenderer.trimStringToWidth(label.get(), Math.max(1, width / 2 - inset - 8));
        String right = minecraft.fontRenderer.trimStringToWidth(value, Math.max(1, width / 2 - 18));
        minecraft.fontRenderer.drawString(left, inset, y, 0xFFFFFF);
        minecraft.fontRenderer.drawString(right,
                Math.max(inset, width - minecraft.fontRenderer.getStringWidth(right) - 18), y, 0xE0E0E0);
    }

    private static void drawArrow(int width, int height) {
        int x = Math.max(4, width - 10);
        int y = Math.max(3, height / 2 - 2);
        Gui.drawRect(x, y, x + 5, y + 1, 0xFFB8C2D0);
        Gui.drawRect(x + 1, y + 1, x + 4, y + 2, 0xFFB8C2D0);
        Gui.drawRect(x + 2, y + 2, x + 3, y + 3, 0xFFB8C2D0);
    }
}
