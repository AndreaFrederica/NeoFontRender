package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrTextButton;

import java.util.ArrayList;
import java.util.List;

/** Complete action list with capture, clear, reset, and conflict diagnostics. */
final class ControllerBindingsPanel extends ParentWidget<ControllerBindingsPanel>
        implements ILayoutWidget {
    private static final int HEADER_HEIGHT = 52;
    private static final int ROW_HEIGHT = 25;
    private static final int GAP = 2;
    private static final int FOOTER_HEIGHT = 28;
    private final ControllerWorkbenchModel model;
    private final ControllerBindingGroup group;
    private final List<BindingRow> rows = new ArrayList<>();
    private final NfrTextButton reset;
    private ControllerBindingCapture capture;

    ControllerBindingsPanel(ControllerWorkbenchModel model, ControllerBindingGroup group) {
        this.model = model;
        this.group = group;
        for (ControllerBindingSpec spec : ControllerBindings.all()) {
            if (!group.contains(spec.getAction())) continue;
            BindingRow row = new BindingRow(spec);
            rows.add(row);
            child(row);
        }
        reset = new NfrTextButton(() -> ControllerText.tr("gui.restore_defaults"), true)
                .onMousePressed(button -> {
                    if (button != 0) return false;
                    capture = null;
                    ControllerBindings.resetDefaults();
                    return true;
                });
        child(reset);
    }

    int preferredHeight() {
        return HEADER_HEIGHT + rows.size() * (ROW_HEIGHT + GAP) + FOOTER_HEIGHT;
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int y = HEADER_HEIGHT;
        for (BindingRow row : rows) {
            NfrLayout.place(row, 0, y, width, ROW_HEIGHT);
            y += ROW_HEIGHT + GAP;
        }
        NfrLayout.place(reset, 0, y + 4, Math.min(180, width), 24);
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        ControllerSnapshot snapshot = model.snapshot();
        if (capture != null) {
            ResourceLocation captured = capture.update(snapshot);
            if (captured != null) {
                ControllerBindings.assign(capture.bindingKey(), captured);
                capture = null;
            }
        }
        super.draw(context, widgetTheme);
        ControllerArc3D.draw(canvas -> drawHeader(canvas, snapshot, getArea().w()));
    }

    private void drawHeader(FlightHudCanvas canvas, ControllerSnapshot snapshot, int width) {
        canvas.text(group.title(), 0, 4, 0.82F,
                0xFFF1F5F9, 0xD0000000);
        String state;
        int color;
        if (capture != null) {
            state = capture.isArmed() ? ControllerText.tr("gui.capture_input")
                    : ControllerText.tr("gui.release_controls");
            color = 0xFFFFD166;
        } else if (!snapshot.isConnected()) {
            state = ControllerText.tr("gui.bindings_no_device");
            color = 0xFFFF9D8D;
        } else {
            state = ControllerText.tr("gui.bindings_help");
            color = 0xFF99A8B8;
        }
        canvas.text(state, 0, 20, 0.65F, color, 0xC0000000);
        canvas.line(0, 43, width, 43, 0x556A7787, 1.0F);
    }

    private void beginCapture(String key) {
        if (capture != null && capture.bindingKey().equals(key)) {
            capture = null;
            return;
        }
        capture = new ControllerBindingCapture(key, model.snapshot());
    }

    private ControllerBindingSpec current(String key) {
        for (ControllerBindingSpec spec : ControllerBindings.all()) {
            if (spec.key().equals(key)) return spec;
        }
        return null;
    }

    private final class BindingRow extends ParentWidget<BindingRow> implements ILayoutWidget {
        private final String key;
        private final neofontrender.addons.api.input.InputAction action;
        private final int slot;
        private final NfrTextButton bind;
        private final NfrTextButton clear;

        private BindingRow(ControllerBindingSpec initial) {
            this.key = initial.key();
            this.action = initial.getAction();
            this.slot = initial.getSlot();
            this.bind = new NfrTextButton(this::bindingLabel, false)
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        beginCapture(key);
                        return true;
                    });
            this.clear = new NfrTextButton(() -> "x", true)
                    .onMousePressed(button -> {
                        if (button != 0) return false;
                        if (capture != null && capture.bindingKey().equals(key)) capture = null;
                        ControllerBindings.assign(key, null);
                        return true;
                    });
            child(bind);
            child(clear);
        }

        @Override
        public boolean layoutWidgets() {
            int width = getArea().w();
            int actionWidth = Math.max(110, Math.min(230, width * 43 / 100));
            int clearWidth = 24;
            NfrLayout.place(bind, actionWidth, 1,
                    Math.max(1, width - actionWidth - clearWidth - 5), ROW_HEIGHT - 2);
            NfrLayout.place(clear, Math.max(0, width - clearWidth), 1,
                    clearWidth, ROW_HEIGHT - 2);
            return true;
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            ControllerBindingSpec spec = current(key);
            ControllerArc3D.draw(canvas -> drawRow(canvas, spec, getArea().w(), getArea().h()));
            super.draw(context, widgetTheme);
        }

        private void drawRow(FlightHudCanvas canvas, ControllerBindingSpec spec,
                             int width, int height) {
            boolean listening = capture != null && capture.bindingKey().equals(key);
            canvas.fill(0, 0, width, height, listening ? 0x553B3A1E
                    : (action.ordinal() + slot & 1) == 0 ? 0x271A222C : 0x171A222C);
            String label = ControllerText.action(action) + (slot == 0 ? "" : "  #" + (slot + 1));
            canvas.text(label, 5, 8, 0.66F, 0xFFE3E9EF, 0xC0000000);
            if (spec != null && spec.isBound() && ControllerBindings.uses(spec.getControl()) > 1) {
                canvas.text("!", Math.max(96, Math.min(216, width * 43 / 100) - 10),
                        8, 0.68F, 0xFFFFC857, 0xC0000000);
            }
        }

        private String bindingLabel() {
            if (capture != null && capture.bindingKey().equals(key)) {
                return ControllerText.tr("gui.listening");
            }
            ControllerBindingSpec spec = current(key);
            return ControllerText.control(spec == null ? null : spec.getControl());
        }
    }
}
