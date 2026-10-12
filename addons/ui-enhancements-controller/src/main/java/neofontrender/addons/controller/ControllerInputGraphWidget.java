package neofontrender.addons.controller;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.addons.controller.sdl.AxisNormalizer;
import neofontrender.addons.controller.sdl.ControllerSnapshot;

import java.util.Locale;
import java.util.function.Supplier;

/** Real-time raw/filter/mapped history plus the current response function. */
final class ControllerInputGraphWidget extends Widget<ControllerInputGraphWidget> {
    private static final int RAW = 0xFF54C8FF;
    private static final int FILTERED = 0xFFFFD166;
    private static final int MAPPED = 0xFF62E6A7;
    private final ControllerWorkbenchModel model;
    private final Supplier<ResourceLocation> selected;
    private final ControllerInputHistory history = new ControllerInputHistory();

    ControllerInputGraphWidget(ControllerWorkbenchModel model,
                               Supplier<ResourceLocation> selected) {
        this.model = model;
        this.selected = selected;
    }

    void resetHistory() { history.clear(); }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        ResourceLocation control = selected.get();
        ControllerSnapshot snapshot = model.snapshot();
        float raw = snapshot.get(control).getAxis();
        float filtered = AxisNormalizer.applyDeadzone(raw, ControllerConfig.deadzone());
        float mapped = ControllerBindings.preview(control, raw);
        history.add(snapshot.getSampledAtNanos(), raw, filtered, mapped);
        ControllerArc3D.draw(canvas -> drawGraphs(canvas, control, raw, filtered, mapped,
                getArea().w(), getArea().h()));
    }

    private void drawGraphs(FlightHudCanvas canvas, ResourceLocation control, float raw,
                            float filtered, float mapped, int width, int height) {
        canvas.fill(0, 0, width, height, 0xC710151C);
        canvas.outline(0, 0, width, height, 0xAA637083, 1.0F);
        String values = String.format(Locale.ROOT, "%s   RAW %+.3f   DZ %+.3f   MAP %+.3f",
                ControllerText.control(control), raw, filtered, mapped);
        canvas.text(values, 7, 6, 0.66F, 0xFFE6EDF4, 0xD0000000);
        int gap = 8;
        int top = 23;
        int graphHeight = Math.max(20, height - top - 7);
        int historyWidth = Math.max(80, (width - gap) * 3 / 5);
        drawFrame(canvas, 5, top, historyWidth - 5, top + graphHeight,
                ControllerText.tr("gui.input_history"));
        drawHistory(canvas, 9, top + 14, historyWidth - 9, top + graphHeight - 4);

        int responseLeft = historyWidth + gap;
        drawFrame(canvas, responseLeft, top, width - 5, top + graphHeight,
                ControllerText.tr("gui.response_curve"));
        drawResponse(canvas, responseLeft + 5, top + 14, width - 9,
                top + graphHeight - 4, raw);
        canvas.text("RAW", 8, height - 14, 0.57F, RAW, 0xC0000000);
        canvas.text("DZ", 38, height - 14, 0.57F, FILTERED, 0xC0000000);
        canvas.text("MAP", 60, height - 14, 0.57F, MAPPED, 0xC0000000);
    }

    private static void drawFrame(FlightHudCanvas canvas, float left, float top,
                                  float right, float bottom, String title) {
        canvas.outline(left, top, right, bottom, 0x665F7184, 1.0F);
        canvas.text(title, left + 4, top + 3, 0.58F, 0xFF9EACBB, 0xC0000000);
    }

    private void drawHistory(FlightHudCanvas canvas, float left, float top,
                             float right, float bottom) {
        float center = (top + bottom) * 0.5F;
        canvas.line(left, center, right, center, 0x555F7184, 1.0F);
        canvas.line(left, top, left, bottom, 0x335F7184, 1.0F);
        if (history.size() < 2) return;
        canvas.polyline(points(left, top, right, bottom, 0), RAW, 1.2F);
        canvas.polyline(points(left, top, right, bottom, 1), FILTERED, 1.25F);
        canvas.polyline(points(left, top, right, bottom, 2), MAPPED, 1.4F);
    }

    private float[] points(float left, float top, float right, float bottom, int series) {
        float[] points = new float[history.size() * 2];
        float center = (top + bottom) * 0.5F;
        float amplitude = Math.max(1.0F, (bottom - top) * 0.5F);
        for (int index = 0; index < history.size(); index++) {
            float fraction = history.size() <= 1 ? 1.0F : index / (float) (history.size() - 1);
            float value = series == 0 ? history.raw(index)
                    : series == 1 ? history.filtered(index) : history.mapped(index);
            points[index * 2] = left + fraction * (right - left);
            points[index * 2 + 1] = center - value * amplitude;
        }
        return points;
    }

    private void drawResponse(FlightHudCanvas canvas, float left, float top,
                              float right, float bottom, float raw) {
        float centerX = (left + right) * 0.5F;
        float centerY = (top + bottom) * 0.5F;
        canvas.line(left, centerY, right, centerY, 0x555F7184, 1.0F);
        canvas.line(centerX, top, centerX, bottom, 0x555F7184, 1.0F);
        int samples = 65;
        float[] filtered = new float[samples * 2];
        float[] mapped = new float[samples * 2];
        ResourceLocation control = selected.get();
        for (int index = 0; index < samples; index++) {
            float input = -1.0F + index * 2.0F / (samples - 1);
            float x = left + (input + 1.0F) * 0.5F * (right - left);
            filtered[index * 2] = x;
            filtered[index * 2 + 1] = centerY - AxisNormalizer.applyDeadzone(
                    input, ControllerConfig.deadzone()) * (bottom - top) * 0.5F;
            mapped[index * 2] = x;
            mapped[index * 2 + 1] = centerY - ControllerBindings.preview(
                    control, input) * (bottom - top) * 0.5F;
        }
        canvas.polyline(filtered, FILTERED, 1.1F);
        canvas.polyline(mapped, MAPPED, 1.5F);
        float markerX = left + (raw + 1.0F) * 0.5F * (right - left);
        float markerY = centerY - ControllerBindings.preview(control, raw)
                * (bottom - top) * 0.5F;
        canvas.circle(markerX, markerY, 2.5F, RAW, 2.5F, 14);
    }
}
