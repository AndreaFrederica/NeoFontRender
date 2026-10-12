package neofontrender.addons.navigation.vanilla;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.Gui;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaControlIntrospectorTest {
    @Test void findsDirectStandardControlsWithStableFieldPathsAndIdentityDeduplication() {
        TestEntry entry = new TestEntry();

        List<VanillaControlIntrospector.ControlRef> controls =
                VanillaControlIntrospector.controls(entry);

        assertEquals(3, controls.size());
        assertSame(entry.left, controls.get(0).widget);
        assertSame(entry.middle, controls.get(1).widget);
        assertSame(entry.right, controls.get(2).widget);
        assertTrue(controls.get(0).path.endsWith("#left"));
        assertTrue(controls.get(1).path.endsWith("#middle"));
        assertTrue(controls.get(2).path.endsWith("#right"));
    }

    private static final class TestEntry {
        private final GuiButton left = new GuiButton(1, 0, 0, "Left");
        private final GuiButton leftAlias = left;
        private final Gui middle = new GuiButton(3, 0, 0, "Generic");
        private final GuiButton right = new GuiButton(2, 0, 0, "Right");
    }
}
