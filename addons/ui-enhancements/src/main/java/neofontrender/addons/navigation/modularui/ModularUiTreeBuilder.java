package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationGeometry;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationTreeEntry;
import com.cleanroommc.modularui.api.navigation.NavigationTreeView;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNavigationHints;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.navigation.ImmutableUiNode;
import neofontrender.addons.navigation.ImmutableUiTreeSnapshot;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModularUiTreeBuilder {
    private static final ResourceLocation OWNER = new ResourceLocation(NfrUiEnhancements.MOD_ID, "modularui");

    BuildResult build(GuiScreen screen, NavigationTreeView view) {
        Map<String, UiNodeId> ids = new LinkedHashMap<>();
        for (NavigationTreeEntry entry : view.getEntries()) ids.put(entry.getPath(), id(entry.getPath()));

        List<ImmutableUiNode> nodes = new ArrayList<>();
        Map<UiNodeId, String> handles = new LinkedHashMap<>();
        for (NavigationTreeEntry entry : view.getEntries()) {
            NavigationInfo info = ModularWidgetAdapterRegistry.resolve(entry.getWidget(), entry.getInfo());
            NavigationGeometry geometry = entry.getGeometry();
            String group = info.getGroup() == null ? "" : info.getGroup();
            if (group.isEmpty() && info.getRole() == com.cleanroommc.modularui.api.navigation.NavigationRole.TAB) {
                group = "modular-tabs:" + entry.getParentPath();
            }
            UiNavigationHints hints = UiNavigationHints.builder()
                    .group(group)
                    .order(info.getOrder())
                    .primaryAxis(ModularNavigationMappings.axis(info.getPrimaryAxis()))
                    .wrapHorizontal(info.isWrapHorizontal())
                    .wrapVertical(info.isWrapVertical())
                    .trapFocus(info.isTrapFocus())
                    .build();
            ImmutableUiNode.Builder node = ImmutableUiNode.builder(ids.get(entry.getPath()),
                            ModularNavigationMappings.role(info.getRole()))
                    .label(info.getLabel())
                    .bounds(new UiRect(geometry.getLeft(), geometry.getTop(),
                            Math.max(geometry.getLeft(), geometry.getRight()),
                            Math.max(geometry.getTop(), geometry.getBottom())))
                    .visibleBounds(new UiRect(geometry.getVisibleLeft(), geometry.getVisibleTop(),
                            Math.max(geometry.getVisibleLeft(), geometry.getVisibleRight()),
                            Math.max(geometry.getVisibleTop(), geometry.getVisibleBottom())))
                    .navigation(hints)
                    .enabled(entry.isEnabled() && geometry.isTopPanelInteractive())
                    // Clipped scroll children stay navigable so reveal() can bring them onscreen.
                    .visible(entry.isEnabled() && geometry.isTopPanelInteractive())
                    .focusable(info.isFocusable());
            if (entry.getParentPath() != null) node.parent(ids.get(entry.getParentPath()));
            for (String child : entry.getChildren()) node.child(ids.get(child));
            for (NavigationAction action : info.getActions()) {
                UiAction mapped = ModularNavigationMappings.action(action);
                node.action(mapped);
            }
            ImmutableUiNode built = node.build();
            nodes.add(built);
            handles.put(built.id(), entry.getPath());
        }

        List<UiNodeId> roots = new ArrayList<>();
        for (String path : view.getRoots()) roots.add(ids.get(path));
        UiNodeId scope = ids.get(view.getActiveScope());
        long revision = 31L * view.getStructureRevision() + view.getGeometryRevision();
        ImmutableUiTreeSnapshot snapshot = new ImmutableUiTreeSnapshot(revision, screen,
                UiInteractionMode.HYBRID, roots, nodes, null, scope);
        return new BuildResult(snapshot, handles);
    }

    private static UiNodeId id(String path) { return new UiNodeId(OWNER, path); }

    static final class BuildResult {
        final ImmutableUiTreeSnapshot snapshot;
        final Map<UiNodeId, String> handles;

        private BuildResult(ImmutableUiTreeSnapshot snapshot, Map<UiNodeId, String> handles) {
            this.snapshot = snapshot;
            this.handles = handles;
        }
    }
}
