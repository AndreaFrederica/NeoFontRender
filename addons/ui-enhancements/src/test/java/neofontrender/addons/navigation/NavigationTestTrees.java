package neofontrender.addons.navigation;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNavigationHints;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiRole;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class NavigationTestTrees {
    static final ResourceLocation OWNER = new ResourceLocation("test", "navigation");

    private NavigationTestTrees() {}

    static UiNodeId id(String path) { return new UiNodeId(OWNER, path); }

    static ImmutableUiNode root(String path, String... children) {
        ImmutableUiNode.Builder builder = ImmutableUiNode.builder(id(path), UiRole.ROOT);
        for (String child : children) builder.child(id(child));
        return builder.build();
    }

    static ImmutableUiNode button(String path, String parent, int left, int top, int order, String group) {
        return ImmutableUiNode.builder(id(path), UiRole.BUTTON)
                .parent(id(parent))
                .bounds(new UiRect(left, top, left + 20, top + 20))
                .navigation(UiNavigationHints.builder().group(group).order(order).build())
                .focusable(true)
                .build();
    }

    static ImmutableUiTreeSnapshot tree(String root, Collection<? extends UiNode> nodes) {
        return tree(root, nodes, id(root));
    }

    static ImmutableUiTreeSnapshot tree(String root, Collection<? extends UiNode> nodes, UiNodeId scope) {
        return new ImmutableUiTreeSnapshot(1L, null, UiInteractionMode.FOCUS,
                Collections.singletonList(id(root)), nodes, null, scope);
    }

    static List<UiNode> nodes(UiNode... nodes) { return Arrays.asList(nodes); }
}
