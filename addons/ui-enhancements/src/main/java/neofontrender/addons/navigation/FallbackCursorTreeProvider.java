package neofontrender.addons.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSession;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class FallbackCursorTreeProvider {
    private static final Method MOUSE_CLICKED = accessible(ReflectionHelper.findMethod(GuiScreen.class,
            "mouseClicked", "func_73864_a", int.class, int.class, int.class));
    private static final Method MOUSE_RELEASED = accessible(ReflectionHelper.findMethod(GuiScreen.class,
            "mouseReleased", "func_146286_b", int.class, int.class, int.class));
    private static final Method MOUSE_CLICK_MOVE = accessible(ReflectionHelper.findMethod(GuiScreen.class,
            "mouseClickMove", "func_146273_a", int.class, int.class, int.class, long.class));

    private FallbackCursorTreeProvider() {}

    static UiTreeSession open(GuiScreen screen) { return new Session(screen); }

    private static final class Session implements UiTreeSession {
        private final GuiScreen screen;
        private UiTreeSnapshot snapshot;

        private Session(GuiScreen screen) {
            this.screen = screen;
            this.snapshot = ImmutableUiTreeSnapshot.empty(screen);
        }

        @Override public GuiScreen screen() { return screen; }
        @Override public UiTreeSnapshot snapshot() { return snapshot; }
        @Override public UiActionResult perform(UiNodeId node, UiAction action) {
            int button = action == UiAction.ACTIVATE ? 0 : action == UiAction.SECONDARY ? 1 : -1;
            if (button < 0 || Minecraft.getMinecraft().currentScreen != screen) return UiActionResult.IGNORED;
            UiNavigationRuntime runtime = UiNavigationRuntime.instance();
            int x = runtime.renderPointerX(1.0F);
            int y = runtime.renderPointerY(1.0F);
            try {
                MOUSE_CLICKED.invoke(screen, x, y, button);
                MOUSE_RELEASED.invoke(screen, x, y, button);
                return UiActionResult.CHANGED;
            } catch (IllegalAccessException | InvocationTargetException error) {
                UiNavigationDiagnostics.failure("fallback pointer click", screen, error);
                return UiActionResult.FAILED;
            }
        }
        @Override public UiActionResult pointerDown(int x, int y, int button) {
            return invokePointer(MOUSE_CLICKED, "fallback pointer down", x, y, button);
        }
        @Override public UiActionResult pointerMove(int x, int y, int button, long timeSincePress) {
            if (Minecraft.getMinecraft().currentScreen != screen) return UiActionResult.STALE;
            try {
                MOUSE_CLICK_MOVE.invoke(screen, x, y, button, timeSincePress);
                return UiActionResult.CHANGED;
            } catch (IllegalAccessException | InvocationTargetException error) {
                UiNavigationDiagnostics.failure("fallback pointer move", screen, error);
                return UiActionResult.FAILED;
            }
        }
        @Override public UiActionResult pointerUp(int x, int y, int button) {
            return invokePointer(MOUSE_RELEASED, "fallback pointer up", x, y, button);
        }
        @Override public UiActionResult reveal(UiNodeId node) { return UiActionResult.IGNORED; }
        @Override public UiActionResult back() {
            if (Minecraft.getMinecraft().currentScreen != screen) return UiActionResult.STALE;
            Minecraft.getMinecraft().displayGuiScreen(null);
            return UiActionResult.CHANGED;
        }
        @Override public void refresh() { snapshot = ImmutableUiTreeSnapshot.empty(screen); }
        @Override public void close() {}

        private UiActionResult invokePointer(Method method, String operation,
                                             int x, int y, int button) {
            if (Minecraft.getMinecraft().currentScreen != screen) return UiActionResult.STALE;
            try {
                method.invoke(screen, x, y, button);
                return UiActionResult.CHANGED;
            } catch (IllegalAccessException | InvocationTargetException error) {
                UiNavigationDiagnostics.failure(operation, screen, error);
                return UiActionResult.FAILED;
            }
        }
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }
}
