package neofontrender.addons.api.ui.navigation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class UiNavigationHints {
    public static final UiNavigationHints DEFAULT = builder().build();

    private final String group;
    private final int order;
    private final UiAxis primaryAxis;
    private final boolean wrapHorizontal;
    private final boolean wrapVertical;
    private final boolean trapFocus;
    private final boolean preferChildren;
    private final Map<UiDirection, UiNodeId> explicitNeighbors;

    private UiNavigationHints(Builder builder) {
        this.group = builder.group;
        this.order = builder.order;
        this.primaryAxis = builder.primaryAxis;
        this.wrapHorizontal = builder.wrapHorizontal;
        this.wrapVertical = builder.wrapVertical;
        this.trapFocus = builder.trapFocus;
        this.preferChildren = builder.preferChildren;
        this.explicitNeighbors = Collections.unmodifiableMap(new EnumMap<>(builder.explicitNeighbors));
    }

    public String group() { return group; }
    public int order() { return order; }
    public UiAxis primaryAxis() { return primaryAxis; }
    /** Legacy all-axis query. Prefer the axis-specific accessors. */
    public boolean wrap() { return wrapHorizontal && wrapVertical; }
    public boolean wrapHorizontal() { return wrapHorizontal; }
    public boolean wrapVertical() { return wrapVertical; }
    public boolean trapFocus() { return trapFocus; }
    public boolean preferChildren() { return preferChildren; }
    public Map<UiDirection, UiNodeId> explicitNeighbors() { return explicitNeighbors; }
    public UiNodeId explicitNeighbor(UiDirection direction) { return explicitNeighbors.get(direction); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String group = "";
        private int order;
        private UiAxis primaryAxis = UiAxis.NONE;
        private boolean wrapHorizontal;
        private boolean wrapVertical;
        private boolean trapFocus;
        private boolean preferChildren;
        private final EnumMap<UiDirection, UiNodeId> explicitNeighbors = new EnumMap<>(UiDirection.class);

        public Builder group(String value) { group = Objects.requireNonNull(value, "group"); return this; }
        public Builder order(int value) { order = value; return this; }
        public Builder primaryAxis(UiAxis value) { primaryAxis = Objects.requireNonNull(value, "primaryAxis"); return this; }
        public Builder wrap(boolean value) {
            wrapHorizontal = value;
            wrapVertical = value;
            return this;
        }
        public Builder wrapHorizontal(boolean value) { wrapHorizontal = value; return this; }
        public Builder wrapVertical(boolean value) { wrapVertical = value; return this; }
        public Builder trapFocus(boolean value) { trapFocus = value; return this; }
        public Builder preferChildren(boolean value) { preferChildren = value; return this; }
        public Builder neighbor(UiDirection direction, UiNodeId id) {
            explicitNeighbors.put(Objects.requireNonNull(direction, "direction"), Objects.requireNonNull(id, "id"));
            return this;
        }
        public UiNavigationHints build() { return new UiNavigationHints(this); }
    }
}
