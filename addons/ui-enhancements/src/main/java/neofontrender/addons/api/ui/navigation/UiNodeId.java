package neofontrender.addons.api.ui.navigation;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public final class UiNodeId implements Comparable<UiNodeId> {
    private final ResourceLocation namespace;
    private final String path;

    public UiNodeId(ResourceLocation namespace, String path) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.path = requirePath(path);
    }

    public ResourceLocation namespace() { return namespace; }
    public String path() { return path; }

    @Override public int compareTo(UiNodeId other) { return toString().compareTo(other.toString()); }
    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof UiNodeId)) return false;
        UiNodeId other = (UiNodeId) object;
        return namespace.equals(other.namespace) && path.equals(other.path);
    }
    @Override public int hashCode() { return 31 * namespace.hashCode() + path.hashCode(); }
    @Override public String toString() { return namespace + "/" + path; }

    private static String requirePath(String value) {
        Objects.requireNonNull(value, "path");
        String path = value.trim();
        if (path.isEmpty()) throw new IllegalArgumentException("path must not be empty");
        return path;
    }
}
