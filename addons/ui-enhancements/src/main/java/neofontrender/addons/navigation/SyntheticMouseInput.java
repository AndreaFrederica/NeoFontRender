package neofontrender.addons.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjglx.input.Mouse;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Queues controller input in lwjgl3ify's LWJGL2 compatibility mouse pipeline. */
public final class SyntheticMouseInput {
    private static final int MAX_EXPECTED_EVENTS = 128;
    private static final Deque<ExpectedEvent> EXPECTED_EVENTS = new ArrayDeque<>();
    private static final boolean[] BUTTONS = new boolean[16];
    private static boolean currentEventSynthetic;
    private static boolean physicalEventPending;

    private SyntheticMouseInput() {}

    static synchronized boolean move(GuiScreen screen, int x, int y) {
        if (!available(screen)) return false;
        NativePosition position = nativePosition(screen, x, y);
        if (Mouse.getX() == position.eventX && Mouse.getY() == position.eventY) return true;
        expect(ExpectedEvent.move(position.eventX, position.eventY));
        // Keep the native cursor at the synthetic position. lwjgl3ify's SDL warp emits the
        // matching motion event, which then enters Mouse exactly once through the normal path.
        Mouse.setCursorPosition(position.eventX, position.cursorY);
        return true;
    }

    static synchronized boolean button(GuiScreen screen, int x, int y,
                                       int button, boolean pressed) {
        if (!available(screen) || button < 0) return false;
        move(screen, x, y);
        NativePosition position = nativePosition(screen, x, y);
        expect(ExpectedEvent.button(position.eventX, position.eventY, button, pressed));
        Mouse.addButtonEvent(button, pressed);
        if (button < BUTTONS.length) BUTTONS[button] = pressed;
        return true;
    }

    static synchronized boolean scroll(GuiScreen screen, int x, int y, int wheel) {
        if (!available(screen) || wheel == 0) return false;
        move(screen, x, y);
        NativePosition position = nativePosition(screen, x, y);
        expect(ExpectedEvent.wheel(position.eventX, position.eventY));
        Mouse.addWheelEvent((double) wheel);
        return true;
    }

    /** Called immediately after the original GuiScreen input loop advances Mouse.next(). */
    public static synchronized void mouseNext(boolean advanced) {
        if (!advanced) {
            currentEventSynthetic = false;
            return;
        }
        currentEventSynthetic = false;
        int skipped = 0;
        for (ExpectedEvent expected : EXPECTED_EVENTS) {
            if (expected.matchesCurrent()) {
                for (int i = 0; i <= skipped; i++) EXPECTED_EVENTS.removeFirst();
                currentEventSynthetic = true;
                return;
            }
            skipped++;
        }
        // Any other event reaching an open screen came from the physical device.
        physicalEventPending = true;
    }

    /** Reports whether a physical mouse event reached an open screen since the last call. */
    static synchronized boolean consumePhysicalEvent() {
        boolean pending = physicalEventPending;
        physicalEventPending = false;
        return pending;
    }

    public static synchronized boolean isCurrentEventSynthetic() {
        return currentEventSynthetic;
    }

    public static synchronized boolean isButtonDown(int button) {
        if (button < 0) return false;
        return button >= 0 && button < BUTTONS.length && BUTTONS[button]
                || Mouse.isButtonDown(button);
    }

    static synchronized boolean isSyntheticButtonDown(int button) {
        return button >= 0 && button < BUTTONS.length && BUTTONS[button];
    }

    static synchronized void reset() {
        Arrays.fill(BUTTONS, false);
        EXPECTED_EVENTS.clear();
        currentEventSynthetic = false;
        physicalEventPending = false;
    }

    private static NativePosition nativePosition(GuiScreen screen, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int eventX = SyntheticMouseCoordinates.nativeEventX(
                x, screen.width, minecraft.displayWidth);
        int eventY = SyntheticMouseCoordinates.nativeEventY(
                y, screen.height, minecraft.displayHeight);
        int cursorY = Math.max(0, minecraft.displayHeight - eventY);
        return new NativePosition(eventX, eventY, cursorY);
    }

    private static boolean available(GuiScreen screen) {
        return screen != null && Mouse.isCreated() && Minecraft.getMinecraft().currentScreen == screen;
    }

    private static void expect(ExpectedEvent event) {
        while (EXPECTED_EVENTS.size() >= MAX_EXPECTED_EVENTS) EXPECTED_EVENTS.removeFirst();
        EXPECTED_EVENTS.addLast(event);
    }

    private static final class NativePosition {
        private final int eventX;
        private final int eventY;
        private final int cursorY;

        private NativePosition(int eventX, int eventY, int cursorY) {
            this.eventX = eventX;
            this.eventY = eventY;
            this.cursorY = cursorY;
        }
    }

    private static final class ExpectedEvent {
        private static final int MOVE = 0;
        private static final int BUTTON = 1;
        private static final int WHEEL = 2;
        private final int kind;
        private final int x;
        private final int y;
        private final int button;
        private final boolean pressed;

        private ExpectedEvent(int kind, int x, int y, int button, boolean pressed) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.button = button;
            this.pressed = pressed;
        }

        private static ExpectedEvent move(int x, int y) {
            return new ExpectedEvent(MOVE, x, y, -1, false);
        }

        private static ExpectedEvent button(int x, int y, int button, boolean pressed) {
            return new ExpectedEvent(BUTTON, x, y, button, pressed);
        }

        private static ExpectedEvent wheel(int x, int y) {
            return new ExpectedEvent(WHEEL, x, y, -1, false);
        }

        private boolean matchesCurrent() {
            if (Mouse.getEventX() != x || Mouse.getEventY() != y) return false;
            if (kind == BUTTON) {
                return Mouse.getEventButton() == button
                        && Mouse.getEventButtonState() == pressed
                        && Mouse.getEventDWheel() == 0;
            }
            if (Mouse.getEventButton() != -1 || Mouse.getEventButtonState()) return false;
            return kind == WHEEL ? Mouse.getEventDWheel() != 0 : Mouse.getEventDWheel() == 0;
        }
    }
}
