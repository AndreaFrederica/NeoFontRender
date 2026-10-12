package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static neofontrender.addons.navigation.NavigationTestTrees.button;
import static neofontrender.addons.navigation.NavigationTestTrees.id;
import static neofontrender.addons.navigation.NavigationTestTrees.nodes;
import static neofontrender.addons.navigation.NavigationTestTrees.root;
import static neofontrender.addons.navigation.NavigationTestTrees.tree;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiTreeValidatorTest {
    @Test void acceptsClosedParentChildGraph() {
        assertDoesNotThrow(() -> UiTreeValidator.validate(tree("root", nodes(
                root("root", "button"), button("button", "root", 0, 0, 0, "main")))));
    }

    @Test void rejectsMissingAndMismatchedRelationships() {
        UiNode missing = ImmutableUiNode.builder(id("root"), UiRole.ROOT).child(id("missing")).build();
        ImmutableUiTreeSnapshot missingTree = new ImmutableUiTreeSnapshot(1L, null,
                UiInteractionMode.FOCUS, Collections.singletonList(id("root")),
                Collections.singletonList(missing), null, id("root"));
        assertThrows(IllegalArgumentException.class, () -> UiTreeValidator.validate(missingTree));

        UiNode child = button("child", "other", 0, 0, 0, "main");
        assertThrows(IllegalArgumentException.class, () -> UiTreeValidator.validate(
                tree("root", nodes(root("root", "child"), child))));
    }

    @Test void rejectsDuplicateIdsBeforeTheyCanEnterASnapshot() {
        UiNode first = root("root");
        UiNode duplicate = root("root");
        assertThrows(IllegalArgumentException.class, () -> new ImmutableUiTreeSnapshot(1L, null,
                UiInteractionMode.FOCUS, Collections.singletonList(id("root")),
                Arrays.asList(first, duplicate), null, id("root")));
    }
}
