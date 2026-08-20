package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.AbstractScrollWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widget.sizer.AreaResizer;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.menu.Menu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.client.gui.component.base.NfrTextButton;

/** Hot-plug-aware NFR dropdown that chooses the SDL device routed into UIE. */
final class ControllerDeviceDropdown extends ContextMenuButton<ControllerDeviceDropdown> {
    private static final int MENU_BACKGROUND = 0xE010151C;
    private static final int MENU_BORDER = 0xAA637083;
    private final ControllerWorkbenchModel model;

    ControllerDeviceDropdown(ControllerWorkbenchModel model) {
        super("controller_target_device");
        this.model = model;
        requiresClick();
        openCustom();
    }

    @Override
    public void openMenu(boolean soft) {
        if (!isOpen()) setMenu(createFreshMenu());
        super.openMenu(soft);
    }

    private Menu<?> createFreshMenu() {
        ListWidget<IWidget, ?> list = new ListWidget<>()
                .widthRel(1.0F)
                .maxSize(144)
                .background(new Rectangle().color(MENU_BACKGROUND));
        for (ControllerSnapshot device : model.devices()) {
            NfrTextButton option = new NfrTextButton(() -> {
                boolean selected = device.getDeviceId().equals(model.snapshot().getDeviceId());
                return (selected ? "* " : "") + display(device);
            }, false).height(22)
                    .background(new Rectangle().color(0x00000000))
                    .hoverBackground(new Rectangle().color(0xB8333333))
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        model.select(device.getDeviceId());
                        closeMenu(false);
                        return true;
                    });
            option.widthRel(1.0F);
            list.child(option);
        }
        Menu<?> menu = new Menu<>()
                .widthRel(1.0F)
                .topRel(1.0F)
                .coverChildrenHeight()
                .background(new Rectangle().color(MENU_BORDER))
                .padding(1)
                .child(list);
        menu.resizer().relative(new AreaResizer(scrolledAnchor()));
        return menu;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        int width = getArea().w();
        int y = Math.max(0, (getArea().h() - minecraft.fontRenderer.FONT_HEIGHT) / 2);
        String label = ControllerText.tr("gui.target_device");
        String value = model.snapshot().isConnected() ? display(model.snapshot())
                : ControllerText.tr("gui.no_controller");
        String left = minecraft.fontRenderer.trimStringToWidth(label, Math.max(1, width / 3));
        String right = minecraft.fontRenderer.trimStringToWidth(value, Math.max(1, width * 2 / 3 - 24));
        minecraft.fontRenderer.drawString(left, 5, y, 0xFFFFFFFF);
        minecraft.fontRenderer.drawString(right,
                Math.max(5, width - minecraft.fontRenderer.getStringWidth(right) - 19),
                y, 0xFFE1E8F0);
        int arrowX = Math.max(4, width - 10);
        int arrowY = Math.max(3, getArea().h() / 2 - 2);
        Gui.drawRect(arrowX, arrowY, arrowX + 5, arrowY + 1, 0xFFB8C2D0);
        Gui.drawRect(arrowX + 1, arrowY + 1, arrowX + 4, arrowY + 2, 0xFFB8C2D0);
        Gui.drawRect(arrowX + 2, arrowY + 2, arrowX + 3, arrowY + 3, 0xFFB8C2D0);
    }

    private Area scrolledAnchor() {
        Area anchor = getArea().createCopy();
        IWidget ancestor = getParent();
        while (ancestor != null) {
            if (ancestor instanceof AbstractScrollWidget) {
                AbstractScrollWidget<?, ?> scroll = (AbstractScrollWidget<?, ?>) ancestor;
                anchor.offset(-scroll.getScrollX(), -scroll.getScrollY());
            }
            ancestor = ancestor.getParent();
        }
        return anchor;
    }

    private static String display(ControllerSnapshot snapshot) {
        return snapshot.getDeviceName() + (snapshot.isGamepad() ? " [Gamepad]" : " [Joystick]");
    }
}
