package neofontrender.addons.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputValue;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiDirection;
import neofontrender.addons.api.ui.navigation.UiInputModality;
import neofontrender.addons.api.ui.navigation.UiInputSource;
import neofontrender.addons.api.ui.navigation.UiInteractionLease;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNavigationResult;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNavigationApi;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.addons.controller.sdl.SdlDeviceManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.input.Mouse;

import java.util.EnumMap;
import java.util.Map;

/** Maps SDL controls to device-neutral UIE intents without inspecting the active screen. */
public final class ControllerUiInputBridge {
    private static final ResourceLocation CURSOR = new ResourceLocation(
            ControllerAddonMod.MOD_ID, "textures/gui/virtual_mouse.png");
    private static final UiInputSource SOURCE = new UiInputSource(
            new ResourceLocation(ControllerAddonMod.MOD_ID, "gamepad"), UiInputModality.CONTROLLER);
    private final SdlDeviceManager manager;
    private final ControllerVirtualCursor cursor = new ControllerVirtualCursor();
    private final ControllerCursorMotion cursorMotion = new ControllerCursorMotion();
    private final ControllerCursorInputFilter cursorInput = new ControllerCursorInputFilter();
    private final ControllerUiRepeatState repeats = new ControllerUiRepeatState();
    private final ControllerAnalogScroll analogScroll = new ControllerAnalogScroll();
    private final Map<InputAction, Boolean> previousDown = new EnumMap<>(InputAction.class);
    private GuiScreen screen;
    private ControllerSnapshot snapshot = ControllerSnapshot.disconnected();
    private UiInteractionMode mode = UiInteractionMode.CURSOR;
    private UiInteractionLease lease;
    private float cursorAxisX;
    private float cursorAxisY;
    private boolean controllerActive;
    private boolean cursorHidden;
    private long lastRenderNanos;

    public ControllerUiInputBridge(SdlDeviceManager manager) { this.manager = manager; }

    @SubscribeEvent
    public void screenOpened(GuiOpenEvent event) {
        if (event.getGui() != screen) reset(event.getGui());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void physicalMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event.getGui() != screen) return;
        if (UiNavigationApi.isSyntheticMouseEvent()) return;
        if (Mouse.getEventDX() == 0 && Mouse.getEventDY() == 0
                && Mouse.getEventButton() < 0 && Mouse.getEventDWheel() == 0) return;
        controllerActive = false;
        releaseLease();
        setCursorHidden(false);
        initializeAtPhysicalMouse();
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        if (current != screen) reset(current);
        if (screen == null) return;
        GuiScreen inputScreen = screen;

        boolean wasConnected = snapshot.isConnected();
        snapshot = manager.pollSnapshot();
        if (!snapshot.isConnected()) {
            if (wasConnected || controllerActive || lease != null) deactivateController();
            return;
        }
        mode = UiNavigationApi.currentTree().interactionMode();
        cursorAxisX = value(InputAction.GUI_CURSOR_X).getAxis();
        cursorAxisY = value(InputAction.GUI_CURSOR_Y).getAxis();

        boolean analogNavigates = mode != UiInteractionMode.CURSOR && mode != UiInteractionMode.HYBRID;
        boolean up = down(InputAction.GUI_NAV_UP) || analogNavigates && cursorAxisY <= -0.55F;
        boolean down = down(InputAction.GUI_NAV_DOWN) || analogNavigates && cursorAxisY >= 0.55F;
        boolean left = down(InputAction.GUI_NAV_LEFT) || analogNavigates && cursorAxisX <= -0.55F;
        boolean right = down(InputAction.GUI_NAV_RIGHT) || analogNavigates && cursorAxisX >= 0.55F;

        boolean used = navigate(ControllerUiRepeatState.Pulse.UP, up, UiDirection.UP);
        used |= navigate(ControllerUiRepeatState.Pulse.DOWN, down, UiDirection.DOWN);
        used |= navigate(ControllerUiRepeatState.Pulse.LEFT, left, UiDirection.LEFT);
        used |= navigate(ControllerUiRepeatState.Pulse.RIGHT, right, UiDirection.RIGHT);

        ActionEdge accept = edge(InputAction.GUI_ACCEPT);
        ActionEdge secondary = edge(InputAction.GUI_SECONDARY);
        ActionEdge quickMove = edge(InputAction.GUI_QUICK_MOVE);
        ActionEdge back = edge(InputAction.GUI_BACK);
        ActionEdge previousPage = edge(InputAction.GUI_PAGE_PREVIOUS);
        ActionEdge nextPage = edge(InputAction.GUI_PAGE_NEXT);
        boolean pointerInteraction = mode == UiInteractionMode.CURSOR
                || mode == UiInteractionMode.HYBRID
                && UiNavigationApi.focusState().focusedNodeId() == null;
        if (pointerInteraction && (accept.pressed || accept.released)) {
            activate();
            UiNavigationApi.pointerButton(0, accept.pressed, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        } else if (accept.pressed) {
            activate();
            UiNavigationApi.perform(UiAction.ACTIVATE, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        }
        if (pointerInteraction && (secondary.pressed || secondary.released)) {
            activate();
            UiNavigationApi.pointerButton(1, secondary.pressed, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        } else if (secondary.pressed) {
            activate();
            UiNavigationApi.perform(UiAction.SECONDARY, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        }
        if (quickMove.pressed) {
            activate();
            UiNavigationApi.perform(UiAction.QUICK_MOVE, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        }
        if (back.pressed) {
            activate();
            UiNavigationApi.back(SOURCE);
            used = true;
            if (screen != inputScreen) return;
        }
        if (previousPage.pressed) {
            activate();
            UiNavigationApi.perform(UiAction.PAGE_PREVIOUS, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        }
        if (nextPage.pressed) {
            activate();
            UiNavigationApi.perform(UiAction.PAGE_NEXT, SOURCE);
            used = true;
            if (screen != inputScreen) return;
        }

        if ((mode == UiInteractionMode.CURSOR || mode == UiInteractionMode.HYBRID)
                && (Math.abs(cursorAxisX) > 0.01F || Math.abs(cursorAxisY) > 0.01F)) used = true;
        if (used) activate();
    }

    // Update before screens such as ModularUI draw and cancel the Pre event at LOW priority.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void beforeDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (event.getGui() != screen) return;
        ControllerSnapshot renderSnapshot = manager.latestSnapshot();
        if (!renderSnapshot.isConnected()) return;
        long now = System.nanoTime();
        double seconds = lastRenderNanos == 0L ? 0.0D
                : Math.min(0.05D, Math.max(0.0D, (now - lastRenderNanos) / 1_000_000_000.0D));
        lastRenderNanos = now;

        cursorInput.update(value(InputAction.GUI_CURSOR_X, renderSnapshot).getAxis(),
                value(InputAction.GUI_CURSOR_Y, renderSnapshot).getAxis(), seconds,
                ControllerConfig.cursorSmoothing());
        cursorAxisX = cursorInput.x();
        cursorAxisY = cursorInput.y();
        int scrollSteps = analogScroll.update(
                value(InputAction.GUI_SCROLL_Y, renderSnapshot).getAxis(), seconds);
        if (scrollSteps != 0) {
            activate();
            boolean pointerInteraction = mode == UiInteractionMode.CURSOR
                    || mode == UiInteractionMode.HYBRID
                    && UiNavigationApi.focusState().focusedNodeId() == null;
            if (pointerInteraction) {
                UiNavigationApi.scrollPointer(-scrollSteps, SOURCE);
            } else {
                UiAction action = scrollSteps < 0 ? UiAction.SCROLL_UP : UiAction.SCROLL_DOWN;
                for (int step = 0; step < Math.abs(scrollSteps); step++) {
                    UiNavigationApi.perform(action, SOURCE);
                }
            }
        }

        double speed = cursorMotion.update(cursorAxisX, cursorAxisY, seconds,
                ControllerConfig.cursorBaseSpeed(), ControllerConfig.cursorMaxSpeed(),
                ControllerConfig.cursorAcceleration());
        if (mode != UiInteractionMode.CURSOR && mode != UiInteractionMode.HYBRID) return;
        if (seconds <= 0.0D || Math.abs(cursorAxisX) <= 0.01F && Math.abs(cursorAxisY) <= 0.01F) return;

        activate();
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        double distance = speed * ControllerConfig.cursorSensitivity() * seconds;
        cursor.move(cursorAxisX, cursorAxisY, distance,
                resolution.getScaledWidth(), resolution.getScaledHeight());
        UiNavigationApi.movePointer(cursor.xDouble(), cursor.yDouble(), SOURCE);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!controllerActive || event.getGui() != screen
                || mode != UiInteractionMode.CURSOR && mode != UiInteractionMode.HYBRID) return;
        if (mode == UiInteractionMode.HYBRID
                && UiNavigationApi.focusState().focusedNodeId() != null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) cursor.xDouble() - 8.0F,
                (float) cursor.yDouble() - 8.0F, 650.0F);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager().bindTexture(CURSOR);
        Gui.drawModalRectWithCustomSizedTexture(0, 0,
                0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private boolean navigate(ControllerUiRepeatState.Pulse pulse, boolean down, UiDirection direction) {
        if (!repeats.pulse(pulse, down)) return false;
        activate();
        UiNavigationResult result = UiNavigationApi.navigate(direction, SOURCE);
        if (result == UiNavigationResult.MOVED) syncCursorToFocus();
        return true;
    }

    private void syncCursorToFocus() {
        UiNode node = UiNavigationApi.currentTree().node(
                UiNavigationApi.focusState().focusedNodeId());
        if (node == null || node.visibleBounds().isEmpty()) return;
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        cursor.set(node.visibleBounds().centerX(), node.visibleBounds().centerY(),
                resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    private void activate() {
        controllerActive = true;
        if (lease == null || !lease.isActive()) {
            releaseLease();
            lease = UiNavigationApi.acquire(SOURCE);
        }
        setCursorHidden(true);
        if (mode == UiInteractionMode.CURSOR) {
            UiNavigationApi.movePointer(cursor.xDouble(), cursor.yDouble(), SOURCE);
        }
    }

    private InputValue value(InputAction action) { return ControllerBindings.resolve(action, snapshot); }
    private InputValue value(InputAction action, ControllerSnapshot source) {
        return ControllerBindings.resolve(action, source);
    }
    private boolean down(InputAction action) { return Math.abs(value(action).getAxis()) >= 0.55F; }

    private ActionEdge edge(InputAction action) {
        boolean down = down(action);
        boolean previous = previousDown.getOrDefault(action, false);
        previousDown.put(action, down);
        return new ActionEdge(down && !previous, !down && previous);
    }

    private void reset(GuiScreen next) {
        screen = next;
        snapshot = ControllerSnapshot.disconnected();
        mode = UiInteractionMode.CURSOR;
        cursorAxisX = 0.0F;
        cursorAxisY = 0.0F;
        controllerActive = false;
        lastRenderNanos = 0L;
        // Preserve held action edges across screens; the next page must wait for release.
        repeats.reset();
        analogScroll.reset();
        cursorMotion.reset();
        cursorInput.reset();
        releaseLease();
        setCursorHidden(false);
        if (next != null) {
            ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
            if (cursor.isInitialized()) {
                cursor.resize(resolution.getScaledWidth(), resolution.getScaledHeight());
            } else {
                initializeAtPhysicalMouse();
            }
        }
    }

    private void deactivateController() {
        if (screen != null) {
            if (previousDown.getOrDefault(InputAction.GUI_ACCEPT, false)) {
                UiNavigationApi.pointerButton(0, false, SOURCE);
            }
            if (previousDown.getOrDefault(InputAction.GUI_SECONDARY, false)) {
                UiNavigationApi.pointerButton(1, false, SOURCE);
            }
        }
        cursorAxisX = 0.0F;
        cursorAxisY = 0.0F;
        controllerActive = false;
        previousDown.clear();
        repeats.reset();
        analogScroll.reset();
        cursorMotion.reset();
        cursorInput.reset();
        releaseLease();
        setCursorHidden(false);
    }

    private void initializeAtPhysicalMouse() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        double x = Mouse.getX() * resolution.getScaledWidth() / (double) Math.max(1, minecraft.displayWidth);
        double y = resolution.getScaledHeight() - Mouse.getY()
                * resolution.getScaledHeight() / (double) Math.max(1, minecraft.displayHeight) - 1.0D;
        cursor.set(x, y, resolution.getScaledWidth(), resolution.getScaledHeight());
    }

    private void releaseLease() {
        if (lease == null) return;
        lease.close();
        lease = null;
    }

    private void setCursorHidden(boolean hidden) {
        if (cursorHidden == hidden) return;
        long window = GLFW.glfwGetCurrentContext();
        if (window == 0L) return;
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR,
                hidden ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL);
        cursorHidden = hidden;
    }

    private static final class ActionEdge {
        private final boolean pressed;
        private final boolean released;

        private ActionEdge(boolean pressed, boolean released) {
            this.pressed = pressed;
            this.released = released;
        }
    }
}
