package neofontrender.addons.controller;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.controller.sdl.ControllerControls;
import neofontrender.addons.controller.sdl.ControllerSnapshot;

/** Arc3D controller tester showing sticks, triggers, D-pad, and live buttons. */
final class ControllerGamepadWidget extends Widget<ControllerGamepadWidget> {
    private static final int PANEL = 0xC710151C;
    private static final int BORDER = 0xAA637083;
    private static final int DIM = 0xFF667382;
    private static final int ACTIVE = 0xFF62E6A7;
    private final ControllerWorkbenchModel model;

    ControllerGamepadWidget(ControllerWorkbenchModel model) { this.model = model; }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        ControllerSnapshot snapshot = model.snapshot();
        ControllerArc3D.draw(canvas -> drawController(canvas, snapshot, getArea().w(), getArea().h()));
    }

    private static void drawController(FlightHudCanvas canvas, ControllerSnapshot snapshot,
                                       int width, int height) {
        canvas.fill(0, 0, width, height, PANEL);
        canvas.outline(0, 0, width, height, BORDER, 1.0F);
        String device = snapshot.isConnected()
                ? snapshot.getDeviceName() + (snapshot.isGamepad() ? "  [Gamepad]" : "  [Joystick]")
                : ControllerText.tr("gui.no_controller");
        canvas.text(ControllerText.tr("gui.input_test"), 7, 6, 0.78F, 0xFFF1F5F9, 0xD0000000);
        canvas.text(device, 7, 18, 0.68F,
                snapshot.isConnected() ? 0xFF9BE8C3 : 0xFFFF9D8D, 0xD0000000);

        // Xbox One layout: left stick upper-left, D-pad below it, right stick lower-right,
        // face buttons upper-right, bumpers/triggers/guide along the top, View/Menu centered.
        float topY = Math.max(66.0F, height * 0.44F);
        float bottomY = Math.min(topY + 52.0F, height - 28.0F);

        trigger(canvas, width * 0.055F, 40, 12, 44,
                axis(snapshot, ControllerControls.LEFT_TRIGGER), "LT");
        trigger(canvas, width * 0.945F - 12, 40, 12, 44,
                axis(snapshot, ControllerControls.RIGHT_TRIGGER), "RT");
        button(canvas, width * 0.36F, 46,
                down(snapshot, ControllerControls.LEFT_SHOULDER), "LB");
        button(canvas, width * 0.64F, 46,
                down(snapshot, ControllerControls.RIGHT_SHOULDER), "RB");
        button(canvas, width * 0.5F, 46, down(snapshot, ControllerControls.GUIDE), "G");

        stick(canvas, width * 0.26F, topY,
                axis(snapshot, ControllerControls.LEFT_STICK_X),
                axis(snapshot, ControllerControls.LEFT_STICK_Y),
                down(snapshot, ControllerControls.LEFT_STICK), "L");
        float faceX = width * 0.76F;
        button(canvas, faceX, topY - 15, down(snapshot, ControllerControls.NORTH), "Y");
        button(canvas, faceX, topY + 15, down(snapshot, ControllerControls.SOUTH), "A");
        button(canvas, faceX - 15, topY, down(snapshot, ControllerControls.WEST), "X");
        button(canvas, faceX + 15, topY, down(snapshot, ControllerControls.EAST), "B");

        float dpadX = width * 0.42F;
        button(canvas, dpadX, bottomY - 13, down(snapshot, ControllerControls.DPAD_UP), "U");
        button(canvas, dpadX, bottomY + 13, down(snapshot, ControllerControls.DPAD_DOWN), "D");
        button(canvas, dpadX - 13, bottomY, down(snapshot, ControllerControls.DPAD_LEFT), "L");
        button(canvas, dpadX + 13, bottomY, down(snapshot, ControllerControls.DPAD_RIGHT), "R");
        stick(canvas, width * 0.58F, bottomY,
                axis(snapshot, ControllerControls.RIGHT_STICK_X),
                axis(snapshot, ControllerControls.RIGHT_STICK_Y),
                down(snapshot, ControllerControls.RIGHT_STICK), "R");

        button(canvas, width * 0.5F - 16, topY + 22,
                down(snapshot, ControllerControls.BACK), "V");
        button(canvas, width * 0.5F + 16, topY + 22,
                down(snapshot, ControllerControls.START), "M");
    }

    private static void stick(FlightHudCanvas canvas, float x, float y,
                              float axisX, float axisY, boolean pressed, String label) {
        float radius = 25.0F;
        canvas.circle(x, y, radius, pressed ? ACTIVE : DIM, pressed ? 3.0F : 1.2F, 32);
        canvas.line(x - radius, y, x + radius, y, 0x446E7D8E, 1.0F);
        canvas.line(x, y - radius, x, y + radius, 0x446E7D8E, 1.0F);
        float dotX = x + axisX * (radius - 4.0F);
        float dotY = y + axisY * (radius - 4.0F);
        canvas.circle(dotX, dotY, 3.0F, ACTIVE, 3.0F, 16);
        canvas.text(label, x - 3, y - 4, 0.7F,
                pressed ? ACTIVE : 0xFFDDE5ED, 0xC0000000);
    }

    private static void trigger(FlightHudCanvas canvas, float x, float y, float width,
                                float height, float value, String label) {
        float normalized = Math.max(0.0F, Math.min(1.0F, value));
        canvas.outline(x, y, x + width, y + height, DIM, 1.0F);
        canvas.fill(x + 2, y + height - 2 - normalized * (height - 4),
                x + width - 2, y + height - 2, ACTIVE);
        canvas.text(label, x - 1, y - 11, 0.65F, 0xFFDDE5ED, 0xC0000000);
    }

    private static void button(FlightHudCanvas canvas, float x, float y, boolean down,
                               String label) {
        int color = down ? ACTIVE : DIM;
        canvas.circle(x, y, 6.0F, color, down ? 3.0F : 1.2F, 18);
        canvas.centeredText(label, x, y - 3, 0.55F, color, 0xC0000000);
    }

    private static float axis(ControllerSnapshot snapshot,
                              net.minecraft.util.ResourceLocation control) {
        return snapshot.get(control).getAxis();
    }

    private static boolean down(ControllerSnapshot snapshot,
                                net.minecraft.util.ResourceLocation control) {
        InputValue value = snapshot.get(control);
        return value.isDown();
    }
}
