package neofontrender.addons.navigation;

import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiNavigationHints;
import neofontrender.addons.api.ui.navigation.UiNode;
import neofontrender.addons.api.ui.navigation.UiNodeId;
import neofontrender.addons.api.ui.navigation.UiRect;
import neofontrender.addons.api.ui.navigation.UiRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ImmutableUiNode implements UiNode {
    private final UiNodeId id;
    private final UiNodeId parentId;
    private final UiRole role;
    private final String label;
    private final UiRect bounds;
    private final UiRect visibleBounds;
    private final List<UiNodeId> children;
    private final Set<UiAction> actions;
    private final UiNavigationHints navigation;
    private final boolean enabled;
    private final boolean visible;
    private final boolean focusable;

    private ImmutableUiNode(Builder builder) {
        id = Objects.requireNonNull(builder.id, "id");
        parentId = builder.parentId;
        role = Objects.requireNonNull(builder.role, "role");
        label = builder.label;
        bounds = builder.bounds;
        visibleBounds = builder.visibleBounds == null ? bounds : builder.visibleBounds;
        children = Collections.unmodifiableList(new ArrayList<>(builder.children));
        actions = Collections.unmodifiableSet(builder.actions.isEmpty()
                ? EnumSet.noneOf(UiAction.class) : EnumSet.copyOf(builder.actions));
        navigation = builder.navigation;
        enabled = builder.enabled;
        visible = builder.visible;
        focusable = builder.focusable;
    }

    @Override public UiNodeId id() { return id; }
    @Override public UiNodeId parentId() { return parentId; }
    @Override public UiRole role() { return role; }
    @Override public String label() { return label; }
    @Override public UiRect bounds() { return bounds; }
    @Override public UiRect visibleBounds() { return visibleBounds; }
    @Override public List<UiNodeId> children() { return children; }
    @Override public Set<UiAction> actions() { return actions; }
    @Override public UiNavigationHints navigation() { return navigation; }
    @Override public boolean enabled() { return enabled; }
    @Override public boolean visible() { return visible; }
    @Override public boolean focusable() { return focusable; }

    public static Builder builder(UiNodeId id, UiRole role) { return new Builder(id, role); }

    public static final class Builder {
        private final UiNodeId id;
        private final UiRole role;
        private UiNodeId parentId;
        private String label = "";
        private UiRect bounds = UiRect.EMPTY;
        private UiRect visibleBounds;
        private final List<UiNodeId> children = new ArrayList<>();
        private final EnumSet<UiAction> actions = EnumSet.noneOf(UiAction.class);
        private UiNavigationHints navigation = UiNavigationHints.DEFAULT;
        private boolean enabled = true;
        private boolean visible = true;
        private boolean focusable;

        private Builder(UiNodeId id, UiRole role) {
            this.id = Objects.requireNonNull(id, "id");
            this.role = Objects.requireNonNull(role, "role");
        }

        public Builder parent(UiNodeId value) { parentId = value; return this; }
        public Builder label(String value) { label = Objects.requireNonNull(value, "label"); return this; }
        public Builder bounds(UiRect value) { bounds = Objects.requireNonNull(value, "bounds"); return this; }
        public Builder visibleBounds(UiRect value) { visibleBounds = Objects.requireNonNull(value, "visibleBounds"); return this; }
        public Builder child(UiNodeId value) { children.add(Objects.requireNonNull(value, "child")); return this; }
        public Builder children(List<UiNodeId> value) { children.addAll(value); return this; }
        public Builder action(UiAction value) { actions.add(Objects.requireNonNull(value, "action")); return this; }
        public Builder actions(Set<UiAction> value) { actions.addAll(value); return this; }
        public Builder navigation(UiNavigationHints value) { navigation = Objects.requireNonNull(value, "navigation"); return this; }
        public Builder enabled(boolean value) { enabled = value; return this; }
        public Builder visible(boolean value) { visible = value; return this; }
        public Builder focusable(boolean value) { focusable = value; return this; }
        public ImmutableUiNode build() { return new ImmutableUiNode(this); }
    }
}
