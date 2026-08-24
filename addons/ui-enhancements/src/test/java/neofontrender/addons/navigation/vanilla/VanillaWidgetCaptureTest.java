package neofontrender.addons.navigation.vanilla;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.api.ui.navigation.UiRect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VanillaWidgetCaptureTest {
    @Test void topLevelCaptureTracksTheCoordinatesActuallyUsedByTheWidget() {
        GuiScreen screen = new GuiScreen() {};
        GuiButton button = new GuiButton(7, 431, 287, 83, 19, "Test");

        VanillaWidgetCapture.beginFrame(screen);
        VanillaWidgetCapture.widgetDrawn(button);
        VanillaWidgetCapture.endFrame(screen);

        List<VanillaWidgetCapture.CapturedWidget> widgets = VanillaWidgetCapture.topLevel(screen);
        assertEquals(1, widgets.size());
        assertSame(button, widgets.get(0).widget);
        assertEquals(new UiRect(431, 287, 514, 306), widgets.get(0).bounds);
    }

    @Test void listCaptureUsesDrawTimeRowOriginsInsteadOfVanillaOffsets() {
        GuiScreen screen = new GuiScreen() {};
        Object entry = new Object();
        GuiButton button = new GuiButton(9, 712, 355, 64, 13, "Nested");

        VanillaWidgetCapture.beginFrame(screen);
        VanillaWidgetCapture.beginListEntry(null, entry, 4, 650, 340);
        VanillaWidgetCapture.widgetDrawn(button);
        VanillaWidgetCapture.endListEntry(null, entry);
        VanillaWidgetCapture.endFrame(screen);

        VanillaWidgetCapture.RelativeGeometry geometry =
                VanillaWidgetCapture.relativeGeometry(screen, button);
        assertEquals(new UiRect(712, 355, 776, 368), geometry.at(650, 340));
        assertEquals(new UiRect(262, 115, 326, 128), geometry.at(200, 100));
    }

    @Test void capturedWidgetIdentityStaysStableWhenItsCoordinatesMove() {
        GuiScreen screen = new GuiScreen() {};
        GuiButton first = new GuiButton(1, 10, 20, "First");
        GuiButton second = new GuiButton(2, 30, 40, "Second");
        long firstId = VanillaWidgetCapture.stableId(screen, first);

        first.x = 900;
        first.y = 700;

        assertEquals(firstId, VanillaWidgetCapture.stableId(screen, first));
        assertNotEquals(firstId, VanillaWidgetCapture.stableId(screen, second));
    }
}
