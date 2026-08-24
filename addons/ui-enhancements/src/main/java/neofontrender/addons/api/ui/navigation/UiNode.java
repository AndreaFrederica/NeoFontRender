package neofontrender.addons.api.ui.navigation;

import java.util.List;
import java.util.Set;

public interface UiNode {
    UiNodeId id();
    UiNodeId parentId();
    UiRole role();
    String label();
    UiRect bounds();
    UiRect visibleBounds();
    List<UiNodeId> children();
    Set<UiAction> actions();
    UiNavigationHints navigation();
    boolean enabled();
    boolean visible();
    boolean focusable();
}
