package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSession;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

final class UiActionDispatcher {
    UiActionResult dispatch(UiTreeSession session, UiTreeSnapshot tree, UiNodeId target, UiAction action) {
        UiNode node = tree.node(target);
        if (node == null) return UiActionResult.STALE;
        if (!node.enabled()) return UiActionResult.REJECTED;
        if (!node.actions().contains(action)) return UiActionResult.REJECTED;
        if (!node.visible() || node.visibleBounds().isEmpty()) {
            UiActionResult reveal = session.reveal(target);
            return reveal.isHandled() ? UiActionResult.DEFERRED : UiActionResult.REJECTED;
        }
        try {
            UiActionResult result = session.perform(target, action);
            return result == null ? UiActionResult.FAILED : result;
        } catch (RuntimeException error) {
            UiNavigationDiagnostics.failure("perform " + action, session.screen(), error);
            return UiActionResult.FAILED;
        }
    }
}
