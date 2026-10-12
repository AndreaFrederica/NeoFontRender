package neofontrender.addons.navigation;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiInputModality;
import neofontrender.addons.api.ui.navigation.UiInputSource;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static neofontrender.addons.navigation.NavigationTestTrees.button;
import static neofontrender.addons.navigation.NavigationTestTrees.id;
import static neofontrender.addons.navigation.NavigationTestTrees.nodes;
import static neofontrender.addons.navigation.NavigationTestTrees.root;
import static neofontrender.addons.navigation.NavigationTestTrees.tree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiFocusManagerTest {
    private static final UiInputSource CONTROLLER = new UiInputSource(
            new ResourceLocation("test", "controller"), UiInputModality.CONTROLLER);

    @Test void restoresNearestSiblingWhenFocusedNodeDisappears() {
        UiFocusManager focus = new UiFocusManager();
        ImmutableUiTreeSnapshot first = tree("root", nodes(
                root("root", "a", "b", "c"),
                button("a", "root", 0, 0, 0, "main"),
                button("b", "root", 30, 0, 1, "main"),
                button("c", "root", 60, 0, 2, "main")));
        focus.updateTree(first);
        assertTrue(focus.focus(first, id("b"), CONTROLLER));

        ImmutableUiTreeSnapshot rebuilt = tree("root", nodes(
                root("root", "a", "c"),
                button("a", "root", 0, 0, 0, "main"),
                button("c", "root", 60, 0, 2, "main")));
        assertEquals(id("c"), focus.updateTree(rebuilt));
        assertTrue(focus.state().focusVisible());
    }

    @Test void restoresLastFocusForModalScope() {
        UiFocusManager focus = new UiFocusManager();
        ImmutableUiTreeSnapshot base = tree("root", nodes(
                root("root", "base"), button("base", "root", 0, 0, 0, "base")));
        focus.updateTree(base);
        focus.focus(base, id("base"), CONTROLLER);

        UiNode modal = ImmutableUiNode.builder(id("modal"), UiRole.MENU)
                .parent(id("root")).child(id("menu-item")).build();
        UiNode rootWithModal = root("root", "base", "modal");
        ImmutableUiTreeSnapshot overlay = tree("root", Arrays.asList(rootWithModal,
                button("base", "root", 0, 0, 0, "base"), modal,
                button("menu-item", "modal", 20, 20, 0, "menu")), id("modal"));
        assertEquals(id("menu-item"), focus.updateTree(overlay));

        assertEquals(id("base"), focus.updateTree(base));
    }

    @Test void pointerClaimHidesControllerFocusWithoutDeletingIt() {
        UiFocusManager focus = new UiFocusManager();
        ImmutableUiTreeSnapshot tree = tree("root", nodes(
                root("root", "button"), button("button", "root", 0, 0, 0, "main")));
        focus.updateTree(tree);
        focus.focus(tree, id("button"), CONTROLLER);
        assertTrue(focus.state().focusVisible());

        focus.claim(new UiInputSource(new ResourceLocation("test", "mouse"), UiInputModality.POINTER));
        assertFalse(focus.state().focusVisible());
        assertEquals(id("button"), focus.state().focusedNodeId());
    }

    @Test void hybridPointerModeSuppressesFocusUntilDirectionalNavigationResumes() {
        UiFocusManager focus = new UiFocusManager();
        ImmutableUiTreeSnapshot tree = tree("root", nodes(
                root("root", "button"), button("button", "root", 0, 0, 0, "main")));
        focus.updateTree(tree);
        focus.focus(tree, id("button"), CONTROLLER);

        focus.usePointer(CONTROLLER);
        assertEquals(null, focus.updateTree(tree));
        assertFalse(focus.state().focusVisible());

        focus.resumeFocusNavigation();
        assertEquals(id("button"), focus.updateTree(tree));
    }
}
