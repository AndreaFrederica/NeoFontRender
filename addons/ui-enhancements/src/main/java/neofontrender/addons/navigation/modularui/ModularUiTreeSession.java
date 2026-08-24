package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.navigation.NavigationTreeEntry;
import com.cleanroommc.modularui.api.navigation.NavigationTreeView;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.navigation.ModularNavigationAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSession;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import neofontrender.addons.navigation.UiNavigationRuntime;

import java.util.Collections;
import java.util.Map;

final class ModularUiTreeSession implements UiTreeSession {
    private final GuiScreen screen;
    private final ModularScreen modularScreen;
    private final ModularUiTreeBuilder builder = new ModularUiTreeBuilder();
    private UiTreeSnapshot snapshot;
    private NavigationTreeView view;
    private Map<UiNodeId, String> handles = Collections.emptyMap();
    private long structureRevision = Long.MIN_VALUE;
    private long geometryRevision = Long.MIN_VALUE;
    private boolean closed;

    ModularUiTreeSession(GuiScreen screen, ModularScreen modularScreen) {
        this.screen = screen;
        this.modularScreen = modularScreen;
        refresh();
    }

    @Override public GuiScreen screen() { return screen; }
    @Override public UiTreeSnapshot snapshot() { return snapshot; }

    @Override public UiActionResult perform(UiNodeId node, UiAction action) {
        if (closed) return UiActionResult.STALE;
        if (node == null) node = pointerTarget(action);
        String path = handles.get(node);
        if (path == null) return UiActionResult.IGNORED;
        return ModularNavigationMappings.result(ModularNavigationAccess.perform(
                modularScreen, path, ModularNavigationMappings.action(action)));
    }

    @Override public UiActionResult pointerDown(int x, int y, int button) {
        if (closed) return UiActionResult.STALE;
        updatePointer(x, y);
        return modularScreen.onMousePressed(button) ? UiActionResult.CHANGED : UiActionResult.HANDLED;
    }

    @Override public UiActionResult pointerMove(int x, int y, int button, long timeSincePress) {
        if (closed) return UiActionResult.STALE;
        updatePointer(x, y);
        return modularScreen.onMouseDrag(button, timeSincePress)
                ? UiActionResult.CHANGED : UiActionResult.HANDLED;
    }

    @Override public UiActionResult pointerUp(int x, int y, int button) {
        if (closed) return UiActionResult.STALE;
        updatePointer(x, y);
        return modularScreen.onMouseRelease(button) ? UiActionResult.CHANGED : UiActionResult.HANDLED;
    }

    @Override public UiActionResult pointerScroll(int x, int y, int wheel) {
        if (closed || wheel == 0) return closed ? UiActionResult.STALE : UiActionResult.IGNORED;
        updatePointer(x, y);
        return modularScreen.onMouseScroll(wheel > 0 ? UpOrDown.UP : UpOrDown.DOWN, Math.abs(wheel))
                ? UiActionResult.CHANGED : UiActionResult.IGNORED;
    }

    private void updatePointer(int x, int y) {
        modularScreen.getContext().updateState(
                x, y, Minecraft.getMinecraft().getRenderPartialTicks());
    }

    private UiNodeId pointerTarget(UiAction action) {
        UiNavigationRuntime runtime = UiNavigationRuntime.instance();
        int x = runtime.renderPointerX(1.0F);
        int y = runtime.renderPointerY(1.0F);
        UiNode best = null;
        int bestDepth = -1;
        long bestArea = Long.MAX_VALUE;
        for (UiNode candidate : snapshot.nodes()) {
            if (!candidate.enabled() || !candidate.visible() || !candidate.actions().contains(action)) continue;
            if (x < candidate.visibleBounds().left || x >= candidate.visibleBounds().right
                    || y < candidate.visibleBounds().top || y >= candidate.visibleBounds().bottom) continue;
            int depth = depth(candidate);
            long area = (long) candidate.visibleBounds().width() * candidate.visibleBounds().height();
            if (depth > bestDepth || depth == bestDepth && area < bestArea) {
                best = candidate;
                bestDepth = depth;
                bestArea = area;
            }
        }
        return best == null ? null : best.id();
    }

    private int depth(UiNode node) {
        int depth = 0;
        UiNode cursor = node;
        while (cursor.parentId() != null) {
            cursor = snapshot.node(cursor.parentId());
            if (cursor == null) break;
            depth++;
        }
        return depth;
    }

    @Override public UiActionResult reveal(UiNodeId node) {
        if (closed) return UiActionResult.STALE;
        String path = handles.get(node);
        NavigationTreeEntry entry = path == null || view == null ? null : view.getEntry(path);
        if (entry == null) return UiActionResult.STALE;
        return ModularNavigationAccess.reveal(modularScreen, entry.getWidget())
                ? UiActionResult.CHANGED : UiActionResult.HANDLED;
    }

    @Override public UiActionResult back() {
        if (closed) return UiActionResult.STALE;
        if (modularScreen.getPanelManager().getOpenPanels().size() > 1) {
            modularScreen.getPanelManager().closeTopPanel();
        } else {
            modularScreen.close();
        }
        return UiActionResult.CHANGED;
    }

    @Override public void refresh() {
        if (closed) return;
        long nextStructure = modularScreen.getPanelManager().getNavigationStructureRevision();
        long nextGeometry = modularScreen.getPanelManager().getNavigationGeometryRevision();
        if (snapshot != null && nextStructure == structureRevision && nextGeometry == geometryRevision) return;
        view = ModularNavigationAccess.capture(modularScreen);
        ModularUiTreeBuilder.BuildResult result = builder.build(screen, view);
        snapshot = result.snapshot;
        handles = result.handles;
        structureRevision = view.getStructureRevision();
        geometryRevision = view.getGeometryRevision();
    }

    @Override public void close() {
        closed = true;
        view = null;
        handles = Collections.emptyMap();
    }
}
