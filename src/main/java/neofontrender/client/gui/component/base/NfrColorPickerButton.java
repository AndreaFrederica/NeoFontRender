package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import net.minecraft.client.gui.Gui;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** NFR-styled launcher for ModularUI's native RGB/HSV/hex color picker. */
public final class NfrColorPickerButton extends NfrTextButton {
    private final String name;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final boolean alpha;
    private final Runnable afterChange;
    private IPanelHandler handler;

    public NfrColorPickerButton(String name, Supplier<String> label, IntSupplier getter,
                                IntConsumer setter, boolean alpha, Runnable afterChange) {
        super(label, false);
        this.name = name;
        this.getter = getter;
        this.setter = setter;
        this.alpha = alpha;
        this.afterChange = afterChange;
        onMousePressed(button -> {
            openPicker();
            return true;
        });
    }

    private void openPicker() {
        if (handler == null) {
            ModularPanel parent = getPanel();
            handler = IPanelHandler.simple(parent, (mainPanel, player) -> {
                ColorPickerDialog dialog = new ColorPickerDialog(name + "_picker", color -> {
                    setter.accept(color);
                    afterChange.run();
                }, getter.getAsInt(), alpha);
                dialog.setDraggable(true)
                        .setCloseOnOutOfBoundsClick(true)
                        .background(new Rectangle().color(0xEE080B10))
                        .center();
                return dialog;
            }, true);
        } else {
            // Recreate the dialog so its initial color follows edits made since the last opening.
            handler.deleteCachedPanel();
        }
        handler.openPanel();
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        int color = getter.getAsInt();
        int size = Math.min(14, Math.max(8, getArea().h() - 8));
        int x = getArea().w() - size - 6;
        int y = (getArea().h() - size) / 2;
        Gui.drawRect(x - 1, y - 1, x + size + 1, y + size + 1, 0xFFE0E0E0);
        Gui.drawRect(x, y, x + size, y + size, color);
    }
}
