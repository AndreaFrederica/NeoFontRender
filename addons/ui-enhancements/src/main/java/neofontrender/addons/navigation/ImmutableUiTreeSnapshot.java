package neofontrender.addons.navigation;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.api.ui.navigation.UiInteractionMode;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ImmutableUiTreeSnapshot implements UiTreeSnapshot {
    private final long revision;
    private final GuiScreen screen;
    private final UiInteractionMode interactionMode;
    private final List<UiNodeId> roots;
    private final Map<UiNodeId, UiNode> nodes;
    private final UiNodeId focusedNodeId;
    private final UiNodeId activeScopeId;

    public ImmutableUiTreeSnapshot(long revision, GuiScreen screen, UiInteractionMode interactionMode,
                                   List<UiNodeId> roots, Collection<? extends UiNode> nodes,
                                   UiNodeId focusedNodeId, UiNodeId activeScopeId) {
        this.revision = revision;
        this.screen = screen;
        this.interactionMode = Objects.requireNonNull(interactionMode, "interactionMode");
        this.roots = Collections.unmodifiableList(new ArrayList<>(roots));
        LinkedHashMap<UiNodeId, UiNode> byId = new LinkedHashMap<>();
        for (UiNode node : nodes) {
            UiNode old = byId.put(Objects.requireNonNull(node, "node").id(), node);
            if (old != null) throw new IllegalArgumentException("duplicate node id: " + node.id());
        }
        this.nodes = Collections.unmodifiableMap(byId);
        this.focusedNodeId = focusedNodeId;
        this.activeScopeId = activeScopeId;
    }

    @Override public long revision() { return revision; }
    @Override public GuiScreen screen() { return screen; }
    @Override public UiInteractionMode interactionMode() { return interactionMode; }
    @Override public List<UiNodeId> roots() { return roots; }
    @Override public UiNode node(UiNodeId id) { return nodes.get(id); }
    @Override public Collection<UiNode> nodes() { return nodes.values(); }
    @Override public UiNodeId focusedNodeId() { return focusedNodeId; }
    @Override public UiNodeId activeScopeId() { return activeScopeId; }

    public ImmutableUiTreeSnapshot withFocus(UiNodeId focus, UiNodeId scope) {
        return new ImmutableUiTreeSnapshot(revision, screen, interactionMode, roots, nodes.values(), focus, scope);
    }

    public static ImmutableUiTreeSnapshot empty(GuiScreen screen) {
        return new ImmutableUiTreeSnapshot(0L, screen, UiInteractionMode.CURSOR,
                Collections.emptyList(), Collections.emptyList(), null, null);
    }
}
