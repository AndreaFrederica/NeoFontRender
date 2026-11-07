package neofontrender.addons.muixml.client;

import com.cleanroommc.common.CleanroomEnvironment;
import com.cleanroommc.modularui.api.dom.MuiElement;
import com.cleanroommc.modularui.api.event.InputModifiers;
import com.cleanroommc.modularui.api.event.PointerEvent;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShowcaseInteractionTest {

    @Test
    void eventsStoreBindingsAndRuntimeDomProjectToWidgets() {
        CleanroomEnvironment.setSide(Side.CLIENT);
        ShowcaseScreen screen = ShowcaseScreen.create();
        try {
            MuiElement toggle = require(screen, "#toggle-font");
            toggle.dispatchEvent(leftPointerDown());
            assertEquals(false, screen.getStore().get("fontEnabled"));
            assertEquals("false", toggle.getAttribute("checked"));
            assertEquals(1, screen.getStore().get("events"));

            require(screen, "#nav-components").dispatchEvent(leftPointerDown());
            assertEquals("components", screen.getStore().get("page"));
            assertEquals("true", require(screen, "#page-components").getAttribute("data-active"));

            require(screen, "#add-row").dispatchEvent(leftPointerDown());
            MuiElement runtimeList = require(screen, "#runtime-list");
            assertEquals(1, runtimeList.getChildNodes().size());
            MuiElement runtimeRow = (MuiElement) runtimeList.getChildNodes().get(0);
            IWidget rowWidget = screen.getDocumentController().resolveWidget(runtimeRow.getHandle());
            assertNotNull(rowWidget);
            IWidget listWidget = screen.getDocumentController().resolveWidget(runtimeList.getHandle());
            assertNotNull(listWidget);
            assertSame(rowWidget, listWidget.getChildren().get(0));

            assertInstanceOf(NfrXmlSliderWidget.class, resolve(screen, "#scale-slider"));
            assertInstanceOf(SliderWidget.class, resolve(screen, "#scale-slider"));
            assertInstanceOf(TextFieldWidget.class, resolve(screen, "#sample-input"));
        } finally {
            screen.onClose();
        }
    }

    private static PointerEvent leftPointerDown() {
        return new PointerEvent(PointerEvent.DOWN, 0, 0, 0, 0, 1,
                0, 0, 0L, InputModifiers.NONE);
    }

    private static MuiElement require(ShowcaseScreen screen, String selector) {
        MuiElement element = screen.getDocument().querySelector(selector);
        assertNotNull(element, selector);
        return element;
    }

    private static IWidget resolve(ShowcaseScreen screen, String selector) {
        MuiElement element = require(screen, selector);
        IWidget widget = screen.getDocumentController().resolveWidget(element.getHandle());
        assertNotNull(widget, selector);
        return widget;
    }
}
