package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationGeometry;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationRole;
import com.cleanroommc.modularui.api.navigation.NavigationTreeEntry;
import com.cleanroommc.modularui.api.navigation.NavigationTreeView;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiDirection;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.navigation.UiSpatialNavigator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularUiTreeBuilderTest {

    @Test
    void buildsHybridTreeFromLiveVisibleGeometry() {
        String rootPath = "panel/main";
        String buttonPath = rootPath + "/id/apply";
        NavigationTreeEntry button = new NavigationTreeEntry(buttonPath, rootPath,
                Collections.emptyList(), null,
                NavigationInfo.builder(NavigationRole.BUTTON)
                        .actions(NavigationAction.ACTIVATE).build(),
                new NavigationGeometry(10, 15, 90, 35, 20, 15, 80, 35, true), true);
        NavigationTreeEntry root = new NavigationTreeEntry(rootPath, null,
                Collections.singletonList(buttonPath), null,
                NavigationInfo.builder(NavigationRole.PANEL).focusable(false).build(),
                new NavigationGeometry(0, 0, 100, 100, 0, 0, 100, 100, true), true);
        NavigationTreeView view = new NavigationTreeView(4L, 7L,
                Collections.singletonList(rootPath), Arrays.asList(button, root), rootPath);

        ModularUiTreeBuilder.BuildResult result = new ModularUiTreeBuilder().build(null, view);
        assertEquals(UiInteractionMode.HYBRID, result.snapshot.interactionMode());
        UiNode built = result.snapshot.nodes().stream().filter(UiNode::focusable).findFirst().orElseThrow();
        assertEquals(10, built.bounds().left);
        assertEquals(20, built.visibleBounds().left);
        assertTrue(built.enabled());
        assertTrue(built.visible());
    }

    @Test
    void blocksWidgetsOutsideTopPanel() {
        String path = "panel/background/id/button";
        NavigationTreeEntry entry = new NavigationTreeEntry(path, null,
                Collections.emptyList(), null,
                NavigationInfo.builder(NavigationRole.BUTTON)
                        .actions(NavigationAction.ACTIVATE).build(),
                new NavigationGeometry(1, 2, 11, 12, 1, 2, 11, 12, false), true);
        NavigationTreeView view = new NavigationTreeView(1L, 1L,
                Collections.singletonList(path), Collections.singletonList(entry), path);

        UiNode built = new ModularUiTreeBuilder().build(null, view).snapshot.nodes().iterator().next();
        assertFalse(built.enabled());
        assertFalse(built.visible());
    }

    @Test
    void keepsClippedScrollChildrenAvailableForRevealNavigation() {
        String rootPath = "panel/main";
        String visiblePath = rootPath + "/id/visible";
        String clippedPath = rootPath + "/id/clipped";
        NavigationInfo buttonInfo = NavigationInfo.builder(NavigationRole.BUTTON)
                .group("list").actions(NavigationAction.ACTIVATE).build();
        NavigationTreeEntry visible = new NavigationTreeEntry(visiblePath, rootPath,
                Collections.emptyList(), null, buttonInfo,
                new NavigationGeometry(10, 10, 90, 30, 10, 10, 90, 30, true), true);
        NavigationTreeEntry clipped = new NavigationTreeEntry(clippedPath, rootPath,
                Collections.emptyList(), null, buttonInfo,
                new NavigationGeometry(10, 40, 90, 60, 10, 40, 10, 40, true), true);
        NavigationTreeEntry root = new NavigationTreeEntry(rootPath, null,
                Arrays.asList(visiblePath, clippedPath), null,
                NavigationInfo.builder(NavigationRole.PANEL).focusable(false).build(),
                new NavigationGeometry(0, 0, 100, 35, 0, 0, 100, 35, true), true);
        NavigationTreeView view = new NavigationTreeView(1L, 1L,
                Collections.singletonList(rootPath), Arrays.asList(visible, clipped, root), rootPath);

        ModularUiTreeBuilder.BuildResult result = new ModularUiTreeBuilder().build(null, view);
        UiNode visibleNode = result.snapshot.nodes().stream()
                .filter(node -> visiblePath.equals(result.handles.get(node.id()))).findFirst().orElseThrow();
        UiNode clippedNode = result.snapshot.nodes().stream()
                .filter(node -> clippedPath.equals(result.handles.get(node.id()))).findFirst().orElseThrow();

        assertTrue(clippedNode.visible());
        assertTrue(clippedNode.visibleBounds().isEmpty());
        assertEquals(clippedNode.id(), new UiSpatialNavigator().find(
                result.snapshot, visibleNode.id(), UiDirection.DOWN));
    }
}
