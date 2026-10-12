package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiDirection;
import neofontrender.addons.api.ui.navigation.UiNavigationHints;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiRole;
import org.junit.jupiter.api.Test;

import static neofontrender.addons.navigation.NavigationTestTrees.button;
import static neofontrender.addons.navigation.NavigationTestTrees.id;
import static neofontrender.addons.navigation.NavigationTestTrees.nodes;
import static neofontrender.addons.navigation.NavigationTestTrees.root;
import static neofontrender.addons.navigation.NavigationTestTrees.tree;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UiSpatialNavigatorTest {
    private final UiSpatialNavigator navigator = new UiSpatialNavigator();

    @Test void prefersSameLaneAndSameGroup() {
        ImmutableUiTreeSnapshot tree = tree("root", nodes(
                root("root", "current", "same-lane", "near-diagonal", "other-group"),
                button("current", "root", 0, 0, 0, "main"),
                button("same-lane", "root", 80, 0, 1, "main"),
                button("near-diagonal", "root", 30, 45, 2, "main"),
                button("other-group", "root", 25, 0, 0, "secondary")));

        assertEquals(id("same-lane"), navigator.find(tree, id("current"), UiDirection.RIGHT));
    }

    @Test void explicitNeighborOverridesGeometry() {
        UiNode current = ImmutableUiNode.builder(id("current"), UiRole.BUTTON)
                .parent(id("root"))
                .bounds(new UiRect(0, 0, 20, 20))
                .navigation(UiNavigationHints.builder().group("main")
                        .neighbor(UiDirection.RIGHT, id("explicit")).build())
                .focusable(true).build();
        ImmutableUiTreeSnapshot tree = tree("root", nodes(
                root("root", "current", "nearest", "explicit"), current,
                button("nearest", "root", 30, 0, 0, "main"),
                button("explicit", "root", 200, 100, 1, "other")));

        assertEquals(id("explicit"), navigator.find(tree, id("current"), UiDirection.RIGHT));
    }

    @Test void wrapsOnlyOnTheDeclaredAxisAndKeepsTheCurrentLane() {
        UiNode current = ImmutableUiNode.builder(id("last"), UiRole.BUTTON)
                .parent(id("root"))
                .bounds(new UiRect(100, 40, 120, 60))
                .navigation(UiNavigationHints.builder().group("tabs").order(3)
                        .wrapHorizontal(true).build())
                .focusable(true).build();
        ImmutableUiTreeSnapshot tree = tree("root", nodes(
                root("root", "top-first", "bottom-first", "last", "below"),
                button("top-first", "root", 0, 0, 0, "tabs"),
                button("bottom-first", "root", 0, 40, 2, "tabs"), current,
                button("below", "root", 100, 100, 0, "content")));

        assertEquals(id("bottom-first"), navigator.find(tree, id("last"), UiDirection.RIGHT));
        assertEquals(id("below"), navigator.find(tree, id("last"), UiDirection.DOWN));
    }
}
