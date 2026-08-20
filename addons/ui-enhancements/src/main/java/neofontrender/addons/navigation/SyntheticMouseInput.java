package neofontrender.addons.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Queues controller input in Cleanroom's LWJGL compatibility mouse pipeline. */
public final class SyntheticMouseInput {
    private static final int MAX_EXPECTED_EVENTS = 128;
    private static final Method ADD_BUTTON_EVENT = extension("addButtonEvent", int.class, boolean.class);
    private static final Method ADD_WHEEL_EVENT = extension("addWheelEvent", double.class);
    private static final Method GET_PIXEL_SCALE_FACTOR = extension(
            Display.class, "getPixelScaleFactor");
    private static final Deque<ExpectedEvent> EXPECTED_EVENTS = new ArrayDeque<>();
    private static final boolean[] BUTTONS = new boolean[16];
    private static boolean currentEventSynthetic;

    private SyntheticMouseInput() {}

    static synchronized boolean move(GuiScreen screen, int x, int y) {
        if (!available(screen)) return false;
        NativePosition position = nativePosition(screen, x, y);
        if (Mouse.getX() == position.eventX && Mouse.getY() == position.eventY) return true;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return false;
        expect(ExpectedEvent.move(position.eventX, position.eventY));
        float scale = pixelScaleFactor();
        // Keep GLFW's authoritative cursor position in sync. Display's installed cursor callback
        // then enters Mouse.addMoveEvent exactly once through the normal LWJGLXX input path.
        GLFW.glfwSetCursorPos(window,
                (position.eventX + 0.25D) / scale,
                (position.cursorY + 0.25D) / scale);
        return true;
    }

    static synchronized boolean button(GuiScreen screen, int x, int y,
                                       int button, boolean pressed) {
        if (!available(screen) || button < 0) return false;
        move(screen, x, y);
        NativePosition position = nativePosition(screen, x, y);
        expect(ExpectedEvent.button(position.eventX, position.eventY, button, pressed));
        try {
            ADD_BUTTON_EVENT.invoke(null, button, pressed);
            if (button < BUTTONS.length) BUTTONS[button] = pressed;
            return true;
        } catch (IllegalAccessException | InvocationTargetException error) {
            UiNavigationDiagnostics.failure("queue synthetic mouse button", screen, unwrap(error));
            return false;
        }
    }

    static synchronized boolean scroll(GuiScreen screen, int x, int y, int wheel) {
        if (!available(screen) || wheel == 0) return false;
        move(screen, x, y);
        NativePosition position = nativePosition(screen, x, y);
        expect(ExpectedEvent.wheel(position.eventX, position.eventY));
        try {
            ADD_WHEEL_EVENT.invoke(null, (double) wheel);
            return true;
        } catch (IllegalAccessException | InvocationTargetException error) {
            UiNavigationDiagnostics.failure("queue synthetic mouse wheel", screen, unwrap(error));
            return false;
        }
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
        return screen != null && Mouse.isCreated() && Minecraft.getMinecraft().currentScreen == screen
                && ADD_BUTTON_EVENT != null && ADD_WHEEL_EVENT != null;
    }

    private static void expect(ExpectedEvent event) {
        while (EXPECTED_EVENTS.size() >= MAX_EXPECTED_EVENTS) EXPECTED_EVENTS.removeFirst();
        EXPECTED_EVENTS.addLast(event);
    }

    private static Method extension(String name, Class<?>... parameters) {
        return extension(Mouse.class, name, parameters);
    }

    private static Method extension(Class<?> owner, String name, Class<?>... parameters) {
        try {
            return owner.getMethod(name, parameters);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static float pixelScaleFactor() {
        if (GET_PIXEL_SCALE_FACTOR == null) return 1.0F;
        try {
            Object value = GET_PIXEL_SCALE_FACTOR.invoke(null);
            return value instanceof Number ? Math.max(0.01F, ((Number) value).floatValue()) : 1.0F;
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return 1.0F;
        }
    }

    private static Throwable unwrap(ReflectiveOperationException error) {
        return error instanceof InvocationTargetException
                && ((InvocationTargetException) error).getCause() != null
                ? ((InvocationTargetException) error).getCause() : error;
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
