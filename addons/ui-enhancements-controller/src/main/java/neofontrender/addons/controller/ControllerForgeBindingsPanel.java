package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrTextButton;

import java.util.ArrayList;
import java.util.List;

/** All vanilla and Forge-registered key bindings exposed as controller-capture rows. */
final class ControllerForgeBindingsPanel extends ParentWidget<ControllerForgeBindingsPanel>
        implements ILayoutWidget {
    private static final int HEADER_HEIGHT = 52;
    private static final int ROW_HEIGHT = 25;
    private static final int GAP = 2;
    private final ControllerWorkbenchModel model;
    private final List<BindingRow> rows = new ArrayList<>();
    private ControllerBindingCapture capture;

    ControllerForgeBindingsPanel(ControllerWorkbenchModel model) {
        this.model = model;
        for (KeyBinding binding : ControllerForgeBindings.registered()) {
            BindingRow row = new BindingRow(binding);
            rows.add(row);
            child(row);
        }
    }

    int preferredHeight() {
        return HEADER_HEIGHT + rows.size() * (ROW_HEIGHT + GAP);
    }

    @Override
    public boolean layoutWidgets() {
        int y = HEADER_HEIGHT;
        for (BindingRow row : rows) {
            NfrLayout.place(row, 0, y, getArea().w(), ROW_HEIGHT);
            y += ROW_HEIGHT + GAP;
        }
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ControllerSnapshot snapshot = model.snapshot();
        if (capture != null) {
            ControllerBindingCapture.CapturedInput captured = capture.updateInput(snapshot);
            if (captured != null) {
                ControllerForgeBindings.assign(capture.bindingKey(),
                        new ControllerKeyBindingAssignment(captured.control(),
                                captured.axisDirection()));
                capture = null;
            }
        }
        super.draw(context, widgetTheme);
        ControllerArc3D.draw(canvas -> drawHeader(canvas, snapshot, getArea().w()));
    }

    private void drawHeader(FlightHudCanvas canvas, ControllerSnapshot snapshot, int width) {
        canvas.text(ControllerText.tr("gui.minecraft_bindings"), 0, 4, 0.82F,
                0xFFF1F5F9, 0xD0000000);
        String state = capture == null
                ? ControllerText.tr("gui.minecraft_bindings_help")
                : capture.isArmed() ? ControllerText.tr("gui.capture_input")
                : ControllerText.tr("gui.release_controls");
        int color = capture == null ? (snapshot.isConnected() ? 0xFF99A8B8 : 0xFFFF9D8D)
                : 0xFFFFD166;
        canvas.text(state, 0, 20, 0.65F, color, 0xC0000000);
        canvas.line(0, 43, width, 43, 0x556A7787, 1.0F);
    }

    private void beginCapture(String description) {
        if (capture != null && capture.bindingKey().equals(description)) {
            capture = null;
            return;
        }
        capture = new ControllerBindingCapture(description, model.snapshot());
    }

    private final class BindingRow extends ParentWidget<BindingRow> implements ILayoutWidget {
        private final String description;
        private final String label;
        private final int rowIndex;
        private final NfrTextButton bind;
        private final NfrTextButton clear;

        private BindingRow(KeyBinding binding) {
            this.description = binding.getKeyDescription();
            this.label = translate(binding.getKeyCategory()) + " / " + translate(description);
            this.rowIndex = rows.size();
            this.bind = new NfrTextButton(this::bindingLabel, false)
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        beginCapture(description);
                        return true;
                    });
            this.clear = new NfrTextButton(() -> "x", true)
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        if (capture != null && capture.bindingKey().equals(description)) capture = null;
                        ControllerForgeBindings.clear(description);
                        return true;
                    });
            child(bind);
            child(clear);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int labelWidth = Math.max(150, Math.min(300, width * 52 / 100));
            int clearWidth = 24;
            NfrLayout.place(bind, labelWidth, 1,
                    Math.max(1, width - labelWidth - clearWidth - 5), ROW_HEIGHT - 2);
            NfrLayout.place(clear, Math.max(0, width - clearWidth), 1,
                    clearWidth, ROW_HEIGHT - 2);
            return true;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            ControllerKeyBindingAssignment assignment =
                    ControllerForgeBindings.assignment(description);
            ResourceLocation control = assignment == null ? null : assignment.control();
            ControllerArc3D.draw(canvas -> drawRow(canvas, control, getArea().w(), getArea().h()));
            super.draw(context, widgetTheme);
        }

        private void drawRow(FlightHudCanvas canvas, ResourceLocation control,
                             int width, int height) {
            boolean listening = capture != null && capture.bindingKey().equals(description);
            canvas.fill(0, 0, width, height, listening ? 0x553B3A1E
                    : (rowIndex & 1) == 0 ? 0x271A222C : 0x171A222C);
            canvas.text(label, 5, 8, 0.62F, 0xFFE3E9EF, 0xC0000000);
            int uses = ControllerForgeBindings.uses(control) + ControllerBindings.uses(control);
            if (control != null && uses > 1) {
                canvas.text("!", Math.max(136, Math.min(286, width * 52 / 100) - 10),
                        8, 0.68F, 0xFFFFC857, 0xC0000000);
            }
        }

        private String bindingLabel() {
            if (capture != null && capture.bindingKey().equals(description)) {
                return ControllerText.tr("gui.listening");
            }
            ControllerKeyBindingAssignment assignment =
                    ControllerForgeBindings.assignment(description);
            return assignment == null ? ControllerText.control(null)
                    : ControllerText.control(assignment.control()) + assignment.directionSuffix();
        }
    }

    private static String translate(String key) {
        String value = I18n.format(key);
        return key.equals(value) ? key : value;
    }
}
