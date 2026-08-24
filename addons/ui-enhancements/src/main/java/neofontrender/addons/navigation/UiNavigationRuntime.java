package neofontrender.addons.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.UiAxis;
import neofontrender.addons.api.ui.navigation.UiDirection;
import neofontrender.addons.api.ui.navigation.UiFocusState;
import neofontrender.addons.api.ui.navigation.UiInputSource;
import neofontrender.addons.api.ui.navigation.UiInteractionLease;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNavigationResult;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRole;
import neofontrender.addons.api.ui.navigation.UiTreeSession;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import neofontrender.addons.navigation.vanilla.VanillaWidgetCapture;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.util.Objects;

public final class UiNavigationRuntime {
    private static final UiNavigationRuntime INSTANCE = new UiNavigationRuntime();

    private final UiFocusManager focus = new UiFocusManager();
    private final UiSpatialNavigator navigator = new UiSpatialNavigator();
    private final UiActionDispatcher dispatcher = new UiActionDispatcher();
    private final UiPointerState pointer = new UiPointerState();
    private GuiScreen currentScreen;
    private ResourceLocation selectedProviderId;
    private ResourceLocation currentProviderId;
    private UiTreeSession currentSession;
    private UiTreeSnapshot currentTree = ImmutableUiTreeSnapshot.empty(null);
    private UiTreeSession pointerCaptureSession;
    private int pointerCaptureButton = -1;
    private long pointerPressMillis;
    private long leaseGeneration;
    private UiInputSource leasedSource;

    private UiNavigationRuntime() {}

    public static UiNavigationRuntime instance() { return INSTANCE; }

    public synchronized UiTreeSnapshot currentTree() {
        ensureMinecraftScreen();
        refresh();
        return currentTree;
    }

    public synchronized UiFocusState focusState() { return focus.state(); }
    synchronized UiTreeSnapshot treeForRender() { return currentTree; }

    public synchronized UiNavigationResult navigate(UiDirection direction, UiInputSource source) {
        ensureMinecraftScreen();
        focus.resumeFocusNavigation();
        refresh();
        if (!supportsFocusNavigation(currentTree.interactionMode())) return UiNavigationResult.REJECTED;
        UiNodeId current = focus.state().focusedNodeId();
        if (current == null) return UiNavigationResult.NO_FOCUS;
        focus.claim(source);

        UiNode node = currentTree.node(current);
        UiAction directionalAction = directionalAction(node, direction);
        if (directionalAction != null) {
            UiActionResult actionResult = dispatcher.dispatch(currentSession, currentTree, current, directionalAction);
            if (actionResult.isHandled()) {
                if (actionResult.isChanged()) refresh();
                return actionResult == UiActionResult.DEFERRED
                        ? UiNavigationResult.DEFERRED : UiNavigationResult.ACTION_HANDLED;
            }
        }

        UiNodeId target = navigator.find(currentTree, current, direction);
        if (target == null) return UiNavigationResult.NO_TARGET;
        UiActionResult reveal = currentSession.reveal(target);
        if (reveal.isChanged()) refresh();
        if (!focus.focus(currentTree, target, source)) {
            return reveal.isChanged() ? UiNavigationResult.DEFERRED : UiNavigationResult.NO_TARGET;
        }
        movePointerToFocus(source);
        attachFocus();
        return UiNavigationResult.MOVED;
    }

    public synchronized UiActionResult perform(UiAction action, UiInputSource source) {
        ensureMinecraftScreen();
        refresh();
        UiNodeId target = focus.state().focusedNodeId();
        if (currentSession == null) return UiActionResult.STALE;
        focus.claim(source);
        target = actionTarget(target, action);
        UiActionResult result;
        if (target == null && (currentTree.interactionMode() == UiInteractionMode.CURSOR
                || currentTree.interactionMode() == UiInteractionMode.HYBRID)) {
            try {
                result = currentSession.perform(null, action);
            } catch (RuntimeException error) {
                UiNavigationDiagnostics.failure("perform pointer " + action, currentScreen, error);
                result = UiActionResult.FAILED;
            }
        } else if (target == null) {
            result = UiActionResult.STALE;
        } else {
            result = dispatcher.dispatch(currentSession, currentTree, target, action);
        }
        if (result.isChanged() || result == UiActionResult.DEFERRED) refresh();
        if (action == UiAction.BEGIN_EDIT && result.isHandled()) focus.editing(true);
        if (action == UiAction.END_EDIT && result.isHandled()) focus.editing(false);
        attachFocus();
        return result;
    }

    public synchronized UiActionResult back(UiInputSource source) {
        ensureMinecraftScreen();
        if (currentSession == null) return UiActionResult.STALE;
        focus.claim(source);
        try {
            UiActionResult result = currentSession.back();
            if (result == null) return UiActionResult.FAILED;
            if (result.isChanged()) refresh();
            return result;
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure("back", currentScreen, error);
            return UiActionResult.FAILED;
        }
    }

    public synchronized void movePointer(double x, double y, UiInputSource source) {
        ensureMinecraftScreen();
        if (currentScreen == null) return;
        double clampedX = Math.max(0.0D, Math.min(Math.max(0, currentScreen.width - 1), x));
        double clampedY = Math.max(0.0D, Math.min(Math.max(0, currentScreen.height - 1), y));
        pointer.move(clampedX, clampedY, source);
        boolean queued = SyntheticMouseInput.move(currentScreen,
                pointer.renderX(1.0F), pointer.renderY(1.0F));
        if (currentTree.interactionMode() == UiInteractionMode.HYBRID) focus.usePointer(source);
        else focus.claim(source);
        if (!queued && pointerCaptureSession == currentSession && pointerCaptureButton >= 0) {
            UiActionResult result = pointerCaptureSession.pointerMove(
                    pointer.renderX(1.0F), pointer.renderY(1.0F), pointerCaptureButton,
                    Math.max(0L, Minecraft.getSystemTime() - pointerPressMillis));
            if (result != null && result.isChanged()) refresh();
        }
    }

    public synchronized UiActionResult pointerButton(int button, boolean pressed, UiInputSource source) {
        ensureMinecraftScreen();
        if (currentSession == null || button < 0) return UiActionResult.STALE;
        focus.usePointer(source);
        int x = pointer.renderX(1.0F);
        int y = pointer.renderY(1.0F);
        if (pressed) {
            if (pointerCaptureSession != null) return UiActionResult.REJECTED;
            UiTreeSession pressedSession = currentSession;
            if (SyntheticMouseInput.button(currentScreen, x, y, button, true)) {
                pointerCaptureSession = pressedSession;
                pointerCaptureButton = button;
                pointerPressMillis = Minecraft.getSystemTime();
                return UiActionResult.DEFERRED;
            }
            UiActionResult result = safePointer("pointer down", () -> pressedSession.pointerDown(x, y, button));
            if (Minecraft.getMinecraft().currentScreen == currentScreen && currentSession == pressedSession
                    && result != UiActionResult.STALE && result != UiActionResult.FAILED) {
                pointerCaptureSession = pressedSession;
                pointerCaptureButton = button;
                pointerPressMillis = Minecraft.getSystemTime();
            }
            if (result.isChanged()) refresh();
            return result;
        }
        boolean syntheticDown = SyntheticMouseInput.isSyntheticButtonDown(button);
        if ((pointerCaptureSession != currentSession || pointerCaptureButton != button) && !syntheticDown) {
            return UiActionResult.IGNORED;
        }
        UiTreeSession releasedSession = pointerCaptureSession;
        clearPointerCapture();
        if (SyntheticMouseInput.button(currentScreen, x, y, button, false)) {
            return UiActionResult.DEFERRED;
        }
        if (releasedSession == null) return UiActionResult.IGNORED;
        UiActionResult result = safePointer("pointer up", () -> releasedSession.pointerUp(x, y, button));
        if (result.isChanged()) refresh();
        return result;
    }

    public synchronized UiActionResult scrollPointer(int wheel, UiInputSource source) {
        ensureMinecraftScreen();
        if (currentSession == null || wheel == 0) return UiActionResult.IGNORED;
        focus.usePointer(source);
        int x = pointer.renderX(1.0F);
        int y = pointer.renderY(1.0F);
        if (SyntheticMouseInput.scroll(currentScreen, x, y, wheel)) {
            return UiActionResult.DEFERRED;
        }
        UiActionResult result = safePointer("pointer scroll", () -> currentSession.pointerScroll(
                x, y, wheel));
        if (result.isChanged()) refresh();
        return result;
    }

    public synchronized UiInteractionLease acquire(UiInputSource source) {
        long token = ++leaseGeneration;
        leasedSource = source;
        focus.claim(source);
        return new Lease(this, source, token);
    }

    public synchronized boolean isSyntheticPointerActive() {
        return currentScreen != null && pointer.isSynthetic();
    }

    public synchronized boolean isSyntheticMouseEvent() {
        return SyntheticMouseInput.isCurrentEventSynthetic();
    }

    public synchronized boolean isPointerButtonDown(int button) {
        return SyntheticMouseInput.isButtonDown(button);
    }

    public synchronized int renderPointerX(float partialTicks) { return pointer.renderX(partialTicks); }
    public synchronized int renderPointerY(float partialTicks) { return pointer.renderY(partialTicks); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void screenOpened(GuiOpenEvent event) { switchScreen(event.getGui()); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public synchronized void beforeDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() != currentScreen) switchScreen(event.getGui());
        VanillaWidgetCapture.beginFrame(event.getGui());
        refresh();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public synchronized void afterDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        VanillaWidgetCapture.endFrame(event.getGui());
        if (event.getGui() == currentScreen) refresh();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public synchronized void physicalMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event.getGui() != currentScreen) return;
        if (SyntheticMouseInput.isCurrentEventSynthetic()) return;
        if (Mouse.getEventDX() == 0 && Mouse.getEventDY() == 0
                && Mouse.getEventButton() < 0 && Mouse.getEventDWheel() == 0) return;
        releasePointerCapture();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        double x = Mouse.getX() * (double) resolution.getScaledWidth()
                / Math.max(1, Minecraft.getMinecraft().displayWidth);
        double y = resolution.getScaledHeight() - Mouse.getY() * (double) resolution.getScaledHeight()
                / Math.max(1, Minecraft.getMinecraft().displayHeight) - 1.0D;
        pointer.physical(x, y);
        focus.claim(null);
        leasedSource = null;
        leaseGeneration++;
    }

    @SubscribeEvent
    public synchronized void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        ensureMinecraftScreen();
        if (!Display.isActive()) releaseInputOwnership();
    }

    private void ensureMinecraftScreen() {
        GuiScreen minecraftScreen = Minecraft.getMinecraft().currentScreen;
        if (minecraftScreen != currentScreen) switchScreen(minecraftScreen);
    }

    private synchronized void switchScreen(GuiScreen next) {
        if (next == currentScreen) return;
        closeSession();
        SyntheticMouseInput.reset();
        currentScreen = next;
        selectedProviderId = null;
        currentProviderId = null;
        focus.clear();
        pointer.clear();
        leasedSource = null;
        leaseGeneration++;
        if (next == null) {
            currentTree = ImmutableUiTreeSnapshot.empty(null);
            return;
        }

        UiNavigationRegistry.Selection selection = null;
        try {
            selection = UiNavigationRegistry.global().select(next);
            selectedProviderId = selection == null ? null : selection.id();
            currentSession = selection == null
                    ? FallbackCursorTreeProvider.open(next) : selection.provider().open(next);
            if (currentSession == null) throw new IllegalStateException("provider returned null session");
            currentProviderId = selection == null ? null : selection.id();
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure(selection == null ? "select provider" : "open provider " + selection.id(),
                    next, error);
            currentSession = FallbackCursorTreeProvider.open(next);
            currentProviderId = null;
        }
        refresh();
    }

    private void refresh() {
        reselectProviderIfNeeded();
        if (currentSession == null) return;
        try {
            currentSession.refresh();
            UiTreeSnapshot raw = Objects.requireNonNull(currentSession.snapshot(), "session snapshot");
            UiTreeValidator.validate(raw);
            UiNodeId focused = focus.updateTree(raw);
            currentTree = new ImmutableUiTreeSnapshot(raw.revision(), raw.screen(), raw.interactionMode(),
                    raw.roots(), raw.nodes(), focused, raw.activeScopeId());
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure("refresh provider " + currentProviderId, currentScreen, error);
            currentTree = ImmutableUiTreeSnapshot.empty(currentScreen);
            focus.updateTree(currentTree);
        }
    }

    private void reselectProviderIfNeeded() {
        if (currentScreen == null || currentSession == null) return;
        UiNavigationRegistry.Selection selection;
        try {
            selection = UiNavigationRegistry.global().select(currentScreen);
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure("reselect provider", currentScreen, error);
            return;
        }
        ResourceLocation nextId = selection == null ? null : selection.id();
        if (Objects.equals(nextId, selectedProviderId)) return;

        closeSession();
        focus.clear();
        selectedProviderId = nextId;
        try {
            currentSession = selection == null
                    ? FallbackCursorTreeProvider.open(currentScreen) : selection.provider().open(currentScreen);
            if (currentSession == null) throw new IllegalStateException("provider returned null session");
            currentProviderId = nextId;
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure(selection == null ? "reselect fallback"
                    : "reselect provider " + selection.id(), currentScreen, error);
            currentSession = FallbackCursorTreeProvider.open(currentScreen);
            currentProviderId = null;
        }
    }

    private void attachFocus() {
        if (!(currentTree instanceof ImmutableUiTreeSnapshot)) return;
        UiFocusState state = focus.state();
        currentTree = ((ImmutableUiTreeSnapshot) currentTree)
                .withFocus(state.focusedNodeId(), state.activeScopeId());
    }

    private void movePointerToFocus(UiInputSource source) {
        UiNode node = currentTree.node(focus.state().focusedNodeId());
        if (node != null && !node.visibleBounds().isEmpty()) {
            pointer.move(node.visibleBounds().centerX(), node.visibleBounds().centerY(), source);
            SyntheticMouseInput.move(currentScreen,
                    pointer.renderX(1.0F), pointer.renderY(1.0F));
        }
    }

    private void releaseInputOwnership() {
        releasePointerCapture();
        pointer.clear();
        focus.release(leasedSource);
        leasedSource = null;
        leaseGeneration++;
    }

    private void closeSession() {
        clearPointerCapture();
        UiTreeSession closing = currentSession;
        currentSession = null;
        if (closing == null) return;
        try {
            closing.close();
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure("close provider " + currentProviderId, currentScreen, error);
        }
    }

    private UiActionResult safePointer(String operation, java.util.function.Supplier<UiActionResult> action) {
        try {
            UiActionResult result = action.get();
            return result == null ? UiActionResult.FAILED : result;
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure(operation, currentScreen, error);
            return UiActionResult.FAILED;
        }
    }

    private void clearPointerCapture() {
        pointerCaptureSession = null;
        pointerCaptureButton = -1;
        pointerPressMillis = 0L;
    }

    private void releasePointerCapture() {
        if (pointerCaptureSession != currentSession || pointerCaptureButton < 0) {
            clearPointerCapture();
            return;
        }
        UiTreeSession captured = pointerCaptureSession;
        int button = pointerCaptureButton;
        int x = pointer.renderX(1.0F);
        int y = pointer.renderY(1.0F);
        clearPointerCapture();
        safePointer("cancel pointer capture", () -> captured.pointerUp(x, y, button));
    }

    private synchronized void releaseLease(UiInputSource source, long token) {
        if (token != leaseGeneration || !source.equals(leasedSource)) return;
        focus.release(source);
        leasedSource = null;
        pointer.clear();
        leaseGeneration++;
    }

    private static boolean supportsFocusNavigation(UiInteractionMode mode) {
        return mode == UiInteractionMode.FOCUS || mode == UiInteractionMode.HYBRID
                || mode == UiInteractionMode.CONTAINER || mode == UiInteractionMode.TEXT_INPUT;
    }

    private static UiAction directionalAction(UiNode node, UiDirection direction) {
        if (node == null) return null;
        boolean horizontal = direction == UiDirection.LEFT || direction == UiDirection.RIGHT;
        boolean adjustable = node.role() == UiRole.SLIDER || node.role() == UiRole.CYCLE;
        if (!horizontal || !adjustable) return null;
        UiAxis axis = node.navigation().primaryAxis();
        if (axis == UiAxis.VERTICAL) return null;
        UiAction action = direction == UiDirection.LEFT ? UiAction.DECREMENT : UiAction.INCREMENT;
        return node.actions().contains(action) ? action : null;
    }

    private UiNodeId actionTarget(UiNodeId focused, UiAction action) {
        UiNode cursor = focused == null ? null : currentTree.node(focused);
        while (cursor != null) {
            if (cursor.actions().contains(action)) return cursor.id();
            if (action != UiAction.SCROLL_UP && action != UiAction.SCROLL_DOWN
                    && action != UiAction.PAGE_PREVIOUS && action != UiAction.PAGE_NEXT) break;
            cursor = cursor.parentId() == null ? null : currentTree.node(cursor.parentId());
        }
        return focused;
    }

    private static final class Lease implements UiInteractionLease {
        private UiNavigationRuntime runtime;
        private final UiInputSource source;
        private final long token;

        private Lease(UiNavigationRuntime runtime, UiInputSource source, long token) {
            this.runtime = runtime;
            this.source = source;
            this.token = token;
        }

        @Override public UiInputSource source() { return source; }
        @Override public boolean isActive() {
            return runtime != null && runtime.leaseGeneration == token
                    && source.equals(runtime.leasedSource);
        }
        @Override public synchronized void close() {
            if (runtime == null) return;
            runtime.releaseLease(source, token);
            runtime = null;
        }
    }
}
