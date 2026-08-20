package neofontrender.addons.controller;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.input.InputAction;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerUiBoundaryTest {
    @Test void legacyGuiScannerClassesAreNotPackaged() {
        ClassLoader loader = ControllerUiBoundaryTest.class.getClassLoader();
        assertNull(loader.getResource(
                "neofontrender/addons/controller/ControllerGuiInputRuntime.class"));
        assertNull(loader.getResource(
                "neofontrender/addons/controller/ControllerGuiNavigation.class"));
        assertNull(loader.getResource(
                "neofontrender/addons/controller/mixin/MixinEntityRendererControllerGui.class"));
    }

    @Test void uiIntentBridgeDoesNotScanVanillaControls() {
        String bridge = bytecode("neofontrender/addons/controller/ControllerUiInputBridge.class");
        String addon = bytecode("neofontrender/addons/controller/ControllerAddonMod.class");
        for (String forbidden : new String[] {
                "net/minecraft/client/gui/GuiButton",
                "net/minecraft/client/gui/GuiSlot",
                "net/minecraft/client/gui/inventory/GuiContainer",
                "buttonList",
                "inventorySlots"
        }) {
            assertFalse(bridge.contains(forbidden), forbidden);
            assertFalse(addon.contains(forbidden), forbidden);
        }
    }

    @Test void controllerMixinConfigHasNoGuiCoordinateHook() {
        String config = resource("mixins.neofontrender_ui_enhancements_controller.json");
        assertFalse(config.contains("MixinEntityRendererControllerGui"));
    }

    @Test void virtualCursorMovesBeforeScreensCanCancelDrawing() throws Exception {
        SubscribeEvent subscription = ControllerUiInputBridge.class
                .getDeclaredMethod("beforeDraw", GuiScreenEvent.DrawScreenEvent.Pre.class)
                .getAnnotation(SubscribeEvent.class);

        assertNotNull(subscription);
        assertEquals(EventPriority.HIGH, subscription.priority());
    }

    @Test void virtualCursorApiSupportsPressDragReleaseAndWheel() throws Exception {
        Class<?> source = Class.forName(
                "neofontrender.addons.api.ui.navigation.UiInputSource");
        Class<?> api = Class.forName(
                "neofontrender.addons.api.ui.navigation.UiNavigationApi");
        assertNotNull(api.getMethod("pointerButton", int.class, boolean.class, source));
        assertNotNull(api.getMethod("scrollPointer", int.class, source));
    }

    @Test
    @SuppressWarnings("unchecked")
    void screenChangesPreserveHeldActionUntilPhysicalRelease() throws Exception {
        ControllerUiInputBridge bridge = new ControllerUiInputBridge(null);
        Field previousDownField = ControllerUiInputBridge.class.getDeclaredField("previousDown");
        previousDownField.setAccessible(true);
        Map<InputAction, Boolean> previousDown =
                (Map<InputAction, Boolean>) previousDownField.get(bridge);
        previousDown.put(InputAction.GUI_ACCEPT, true);

        Method reset = ControllerUiInputBridge.class.getDeclaredMethod(
                "reset", net.minecraft.client.gui.GuiScreen.class);
        reset.setAccessible(true);
        reset.invoke(bridge, new Object[] { null });

        assertTrue(previousDown.get(InputAction.GUI_ACCEPT));
    }

    private static String bytecode(String name) {
        return resource(name, StandardCharsets.ISO_8859_1);
    }

    private static String resource(String name) {
        return resource(name, StandardCharsets.UTF_8);
    }

    private static String resource(String name, java.nio.charset.Charset charset) {
        InputStream stream = ControllerUiBoundaryTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), charset);
        } catch (Exception error) {
            throw new AssertionError("Failed to read " + name, error);
        }
    }
}
