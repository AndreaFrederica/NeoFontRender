package neofontrender.addons.navigation.vanilla;

import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.CreativeTabNavigation;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRole;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaNavigationGeometryTest {
    @Test void listEntryPositionUsesTheListsRuntimeViewportAndWidth() {
        int entryLeft = VanillaGuiTreeSession.listEntryLeft(0, 3885, 252);

        assertEquals(1818, entryLeft);
    }

    @Test void capturedRelativeGeometryProjectsOntoAnyRuntimeRowPosition() {
        VanillaWidgetCapture.RelativeGeometry geometry =
                new VanillaWidgetCapture.RelativeGeometry(37, 3, 91, 17);

        assertEquals(new UiRect(183, 424, 274, 441), geometry.at(146, 421));
    }

    @Test void creativeTabsAreFocusableDirectionalNavigationTargets() {
        UiNodeId listId = new UiNodeId(new ResourceLocation("test", "navigation"), "tabs");
        UiNodeId tabId = new UiNodeId(new ResourceLocation("test", "navigation"), "tabs/tab/4");
        UiRect bounds = new UiRect(20, 30, 49, 63);
        CreativeTabNavigation.Tab tab = new CreativeTabNavigation.Tab(4, "Building Blocks", bounds);

        neofontrender.addons.api.ui.navigation.UiNode node =
                VanillaGuiTreeSession.creativeTabNode(tabId, listId, tab, bounds, 2);

        assertEquals(UiRole.TAB, node.role());
        assertEquals(bounds, node.bounds());
        assertEquals("Building Blocks", node.label());
        assertTrue(node.focusable());
        assertTrue(node.actions().contains(UiAction.ACTIVATE));
        assertTrue(node.navigation().wrapHorizontal());
        assertEquals(false, node.navigation().wrapVertical());
    }

    @Test void containerSlotIdentityUsesListOrdinalInsteadOfNonUniqueSlotNumber() {
        UiNodeId first = VanillaGuiTreeSession.containerSlotId(getClass(), 0);
        UiNodeId second = VanillaGuiTreeSession.containerSlotId(getClass(), 1);

        assertNotEquals(first, second);
    }
}
