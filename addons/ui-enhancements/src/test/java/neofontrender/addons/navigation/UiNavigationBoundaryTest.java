package neofontrender.addons.navigation;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UiNavigationBoundaryTest {
    @Test void uiNavigationCoreHasNoControllerOrSdlDependency() {
        for (String resource : new String[] {
                "neofontrender/addons/navigation/UiNavigationModule.class",
                "neofontrender/addons/navigation/UiNavigationRuntime.class",
                "neofontrender/addons/navigation/UiPointerState.class",
                "neofontrender/addons/navigation/UiActionDispatcher.class"
        }) {
            String bytecode = bytecode(resource);
            assertFalse(bytecode.contains("neofontrender/addons/controller"), resource);
            assertFalse(bytecode.contains("dev/isxander/sdl"), resource);
        }
    }

    private static String bytecode(String name) {
        InputStream stream = UiNavigationBoundaryTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull(stream, name);
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
        } catch (Exception error) {
            throw new AssertionError("Failed to inspect " + name, error);
        }
    }
}
