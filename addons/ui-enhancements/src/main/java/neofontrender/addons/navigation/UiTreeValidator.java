package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

import java.util.HashSet;
import java.util.Set;

public final class UiTreeValidator {
    private UiTreeValidator() {}

    public static void validate(UiTreeSnapshot tree) {
        Set<UiNodeId> seen = new HashSet<>();
        for (UiNode node : tree.nodes()) {
            if (!seen.add(node.id())) fail("duplicate node id " + node.id());
        }
        for (UiNodeId root : tree.roots()) {
            UiNode node = require(tree, root, "missing root ");
            if (node.parentId() != null) fail("root has parent " + root);
        }
        for (UiNode node : tree.nodes()) {
            if (node.parentId() != null) {
                UiNode parent = require(tree, node.parentId(), "missing parent ");
                if (!parent.children().contains(node.id())) fail("parent does not reference child " + node.id());
            } else if (!tree.roots().contains(node.id())) {
                fail("detached root " + node.id());
            }
            Set<UiNodeId> children = new HashSet<>();
            for (UiNodeId childId : node.children()) {
                if (!children.add(childId)) fail("duplicate child " + childId + " in " + node.id());
                UiNode child = require(tree, childId, "missing child ");
                if (!node.id().equals(child.parentId())) fail("child parent mismatch " + childId);
            }
            for (UiNodeId neighbor : node.navigation().explicitNeighbors().values()) {
                require(tree, neighbor, "missing explicit neighbor ");
            }
        }
        if (tree.activeScopeId() != null) require(tree, tree.activeScopeId(), "missing active scope ");
        if (tree.focusedNodeId() != null) {
            UiNode focus = require(tree, tree.focusedNodeId(), "missing focused node ");
            if (!focus.focusable()) fail("focused node is not focusable " + focus.id());
        }
    }

    private static UiNode require(UiTreeSnapshot tree, UiNodeId id, String message) {
        UiNode node = tree.node(id);
        if (node == null) fail(message + id);
        return node;
    }

    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
