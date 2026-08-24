package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.controller.sdl.ControllerControls;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrTextButton;

/** NFR component containing device visualization, axis selection, and Arc3D graphs. */
final class ControllerTestPanel extends ParentWidget<ControllerTestPanel> implements ILayoutWidget {
    static final int HEIGHT = 390;
    private static final int GAP = 6;
    private static final ResourceLocation[] AXES = {
            ControllerControls.LEFT_STICK_X, ControllerControls.LEFT_STICK_Y,
            ControllerControls.RIGHT_STICK_X, ControllerControls.RIGHT_STICK_Y,
            ControllerControls.LEFT_TRIGGER, ControllerControls.RIGHT_TRIGGER
    };
    private static final String[] AXIS_LABELS = { "LX", "LY", "RX", "RY", "LT", "RT" };

    private final ControllerWorkbenchModel model;
    private final ControllerDeviceDropdown deviceSelector;
    private final ControllerGamepadWidget gamepad;
    private final ControllerInputGraphWidget graph;
    private final IWidget[] selectors = new IWidget[AXES.length];
    private ResourceLocation selected = ControllerControls.LEFT_STICK_X;

    ControllerTestPanel(ControllerWorkbenchModel model) {
        this.model = model;
        this.deviceSelector = new ControllerDeviceDropdown(model);
        this.gamepad = new ControllerGamepadWidget(model);
        this.graph = new ControllerInputGraphWidget(model, () -> selected);
        child(deviceSelector);
        child(gamepad);
        for (int index = 0; index < AXES.length; index++) {
            final int axis = index;
            selectors[index] = new NfrTextButton(() -> AXIS_LABELS[axis], true)
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        selected = AXES[axis];
                        graph.resetHistory();
                        return true;
                    });
            child(selectors[index]);
        }
        child(graph);
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int visualizationHeight = 150;
        NfrLayout.place(deviceSelector, 0, 0, width, 24);
        NfrLayout.place(gamepad, 0, 24 + GAP, width, visualizationHeight);
        int selectorY = 24 + GAP + visualizationHeight + GAP;
        int selectorWidth = Math.max(1, (width - GAP * (selectors.length - 1)) / selectors.length);
        int x = 0;
        for (IWidget selector : selectors) {
            NfrLayout.place(selector, x, selectorY, selectorWidth, 22);
            x += selectorWidth + GAP;
        }
        NfrLayout.place(graph, 0, selectorY + 22 + GAP, width,
                Math.max(1, HEIGHT - selectorY - 22 - GAP));
        return true;
    }
}
