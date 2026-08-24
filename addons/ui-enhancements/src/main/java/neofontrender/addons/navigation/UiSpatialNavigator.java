package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiDirection;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class UiSpatialNavigator {
    private static final double GROUP_TRANSITION_PENALTY = 1_000_000.0D;

    public UiNodeId find(UiTreeSnapshot tree, UiNodeId currentId, UiDirection direction) {
        UiNode current = tree.node(currentId);
        if (!eligible(tree, current)) return null;

        UiNodeId explicit = current.navigation().explicitNeighbor(direction);
        if (explicit != null && navigable(tree, tree.node(explicit))) return explicit;

        List<UiNode> sameGroup = candidates(tree, current, direction, true);
        if (!sameGroup.isEmpty()) return best(current, sameGroup, direction, false).id();

        if (wraps(current, direction)) {
            List<UiNode> wrapped = groupMembers(tree, current);
            if (!wrapped.isEmpty()) return wrapped.stream()
                    .min(wrapComparator(current, direction)).get().id();
        }

        List<UiNode> anyGroup = candidates(tree, current, direction, false);
        if (!anyGroup.isEmpty()) return best(current, anyGroup, direction, true).id();
        return null;
    }

    private List<UiNode> candidates(UiTreeSnapshot tree, UiNode current, UiDirection direction,
                                    boolean sameGroup) {
        List<UiNode> result = new ArrayList<>();
        String group = current.navigation().group();
        for (UiNode candidate : tree.nodes()) {
            if (candidate.id().equals(current.id()) || !navigable(tree, candidate)) continue;
            if (sameGroup != group.equals(candidate.navigation().group())) continue;
            if (inDirection(current.visibleBounds(), candidate.visibleBounds(), direction)) result.add(candidate);
        }
        return result;
    }

    private List<UiNode> groupMembers(UiTreeSnapshot tree, UiNode current) {
        List<UiNode> result = new ArrayList<>();
        for (UiNode node : tree.nodes()) {
            if (!node.id().equals(current.id()) && navigable(tree, node)
                    && current.navigation().group().equals(node.navigation().group())) result.add(node);
        }
        return result;
    }

    private UiNode best(UiNode current, List<UiNode> candidates, UiDirection direction,
                        boolean groupPenalty) {
        return candidates.stream().min(Comparator
                .comparingDouble((UiNode candidate) -> score(current, candidate, direction, groupPenalty))
                .thenComparingInt(candidate -> candidate.navigation().order())
                .thenComparing(UiNode::id)).get();
    }

    static double score(UiNode current, UiNode candidate, UiDirection direction, boolean groupPenalty) {
        UiRect from = navigationBounds(current);
        UiRect to = navigationBounds(candidate);
        double dx = to.centerX() - from.centerX();
        double dy = to.centerY() - from.centerY();
        double forward = direction == UiDirection.LEFT || direction == UiDirection.RIGHT ? Math.abs(dx) : Math.abs(dy);
        double cross = direction == UiDirection.LEFT || direction == UiDirection.RIGHT ? Math.abs(dy) : Math.abs(dx);
        double laneSize = direction == UiDirection.LEFT || direction == UiDirection.RIGHT
                ? Math.max(1.0D, (from.height() + to.height()) * 0.5D)
                : Math.max(1.0D, (from.width() + to.width()) * 0.5D);
        double lanePenalty = cross <= laneSize ? cross * 4.0D : cross * 16.0D;
        double euclidean = Math.hypot(dx, dy) * 0.01D;
        double transition = groupPenalty
                && !current.navigation().group().equals(candidate.navigation().group())
                ? GROUP_TRANSITION_PENALTY : 0.0D;
        return transition + lanePenalty + forward + euclidean;
    }

    private static boolean inDirection(UiRect from, UiRect to, UiDirection direction) {
        switch (direction) {
            case LEFT: return to.centerX() < from.centerX();
            case RIGHT: return to.centerX() > from.centerX();
            case UP: return to.centerY() < from.centerY();
            case DOWN: return to.centerY() > from.centerY();
            default: return false;
        }
    }

    private Comparator<UiNode> wrapComparator(UiNode current, UiDirection direction) {
        Comparator<UiNode> position;
        if (direction == UiDirection.RIGHT) position = Comparator.comparingDouble(node -> navigationBounds(node).centerX());
        else if (direction == UiDirection.LEFT) position = Comparator.comparingDouble((UiNode node) -> navigationBounds(node).centerX()).reversed();
        else if (direction == UiDirection.DOWN) position = Comparator.comparingDouble(node -> navigationBounds(node).centerY());
        else position = Comparator.comparingDouble((UiNode node) -> navigationBounds(node).centerY()).reversed();
        Comparator<UiNode> lane = direction == UiDirection.LEFT || direction == UiDirection.RIGHT
                ? Comparator.comparingDouble(node -> Math.abs(navigationBounds(node).centerY()
                        - navigationBounds(current).centerY()))
                : Comparator.comparingDouble(node -> Math.abs(navigationBounds(node).centerX()
                        - navigationBounds(current).centerX()));
        return position.thenComparing(lane)
                .thenComparingInt(node -> node.navigation().order()).thenComparing(UiNode::id);
    }

    private static boolean wraps(UiNode node, UiDirection direction) {
        return direction == UiDirection.LEFT || direction == UiDirection.RIGHT
                ? node.navigation().wrapHorizontal() : node.navigation().wrapVertical();
    }

    static boolean eligible(UiTreeSnapshot tree, UiNode node) {
        return node != null && node.enabled() && node.visible() && node.focusable()
                && !node.visibleBounds().isEmpty() && insideScope(tree, node);
    }

    private static boolean navigable(UiTreeSnapshot tree, UiNode node) {
        return node != null && node.enabled() && node.visible() && node.focusable()
                && !node.bounds().isEmpty() && insideScope(tree, node);
    }

    private static UiRect navigationBounds(UiNode node) {
        return node.visibleBounds().isEmpty() ? node.bounds() : node.visibleBounds();
    }

    private static boolean insideScope(UiTreeSnapshot tree, UiNode node) {
        UiNodeId scope = tree.activeScopeId();
        if (scope == null) return true;
        UiNode cursor = node;
        while (cursor != null) {
            if (scope.equals(cursor.id())) return true;
            cursor = cursor.parentId() == null ? null : tree.node(cursor.parentId());
        }
        return false;
    }
}
