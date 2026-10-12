package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiFocusState;
import neofontrender.addons.api.ui.navigation.UiInputModality;
import neofontrender.addons.api.ui.navigation.UiInputSource;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UiFocusManager {
    private final Map<UiNodeId, UiNodeId> lastFocusedByScope = new HashMap<>();
    private UiTreeSnapshot previousTree;
    private UiNodeId focusedNodeId;
    private UiNodeId activeScopeId;
    private UiInputSource inputSource;
    private boolean focusVisible;
    private boolean editing;
    private boolean pointerMode;

    public UiNodeId updateTree(UiTreeSnapshot tree) {
        UiNodeId nextScope = tree.activeScopeId();
        if (activeScopeId != null && focusedNodeId != null) lastFocusedByScope.put(activeScopeId, focusedNodeId);
        activeScopeId = nextScope;

        if (!pointerMode && !isEligible(tree, focusedNodeId)) {
            UiNodeId restored = nextScope == null ? null : lastFocusedByScope.get(nextScope);
            if (!isEligible(tree, restored)) restored = siblingFallback(tree);
            if (!isEligible(tree, restored)) restored = firstFocusable(tree);
            focusedNodeId = restored;
        }
        if (activeScopeId != null && focusedNodeId != null) lastFocusedByScope.put(activeScopeId, focusedNodeId);
        if (focusedNodeId == null) editing = false;
        previousTree = tree;
        return focusedNodeId;
    }

    public boolean focus(UiTreeSnapshot tree, UiNodeId id, UiInputSource source) {
        if (!isEligible(tree, id)) return false;
        pointerMode = false;
        focusedNodeId = id;
        inputSource = source;
        focusVisible = source != null && source.modality() != UiInputModality.POINTER;
        editing = false;
        if (activeScopeId != null) lastFocusedByScope.put(activeScopeId, id);
        return true;
    }

    public void claim(UiInputSource source) {
        inputSource = source;
        focusVisible = source != null && source.modality() != UiInputModality.POINTER;
    }

    public void usePointer(UiInputSource source) {
        if (activeScopeId != null && focusedNodeId != null) {
            lastFocusedByScope.put(activeScopeId, focusedNodeId);
        }
        focusedNodeId = null;
        inputSource = source;
        focusVisible = false;
        editing = false;
        pointerMode = true;
    }

    public void resumeFocusNavigation() { pointerMode = false; }

    public void release(UiInputSource source) {
        if (source != null && source.equals(inputSource)) {
            inputSource = null;
            focusVisible = false;
            editing = false;
        }
    }

    public void editing(boolean value) { editing = value && focusedNodeId != null; }

    public UiFocusState state() {
        return new UiFocusState(focusedNodeId, activeScopeId, inputSource, focusVisible, editing);
    }

    public void clear() {
        previousTree = null;
        focusedNodeId = null;
        activeScopeId = null;
        inputSource = null;
        focusVisible = false;
        editing = false;
        pointerMode = false;
        lastFocusedByScope.clear();
    }

    private UiNodeId siblingFallback(UiTreeSnapshot tree) {
        if (previousTree == null || focusedNodeId == null) return null;
        UiNode old = previousTree.node(focusedNodeId);
        if (old == null || old.parentId() == null) return null;
        UiNode oldParent = previousTree.node(old.parentId());
        UiNode newParent = tree.node(old.parentId());
        if (oldParent == null || newParent == null) return null;
        int oldIndex = oldParent.children().indexOf(focusedNodeId);
        UiNodeId best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < newParent.children().size(); i++) {
            UiNodeId candidate = newParent.children().get(i);
            if (!isEligible(tree, candidate)) continue;
            int distance = Math.abs(i - Math.max(0, oldIndex));
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private UiNodeId firstFocusable(UiTreeSnapshot tree) {
        List<UiNode> candidates = new ArrayList<>();
        for (UiNode node : tree.nodes()) if (UiSpatialNavigator.eligible(tree, node)) candidates.add(node);
        return candidates.stream().min(Comparator
                .comparingInt((UiNode node) -> node.navigation().order())
                .thenComparingInt(node -> node.visibleBounds().top)
                .thenComparingInt(node -> node.visibleBounds().left)
                .thenComparing(UiNode::id)).map(UiNode::id).orElse(null);
    }

    private static boolean isEligible(UiTreeSnapshot tree, UiNodeId id) {
        return id != null && UiSpatialNavigator.eligible(tree, tree.node(id));
    }
}
