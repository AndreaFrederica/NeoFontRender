package neofontrender.api.text.gl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Registry for standard render-thread GL components used by text post-processors. */
public final class TextGlComponentApi {
    private static final List<TextGlComponent> COMPONENTS = new ArrayList<>();

    private TextGlComponentApi() {}

    public static synchronized void register(TextGlComponent component) {
        if (component == null || component.id() == null || component.id().isEmpty()) {
            throw new IllegalArgumentException("component must have an id");
        }
        COMPONENTS.removeIf(existing -> existing.id().equals(component.id()));
        COMPONENTS.add(component);
        COMPONENTS.sort(Comparator.comparingInt(TextGlComponent::priority).reversed()
                .thenComparing(TextGlComponent::id));
    }

    public static synchronized TextGlComponent active() {
        for (TextGlComponent component : COMPONENTS) {
            try {
                if (component.isAvailable()) return component;
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return null;
    }

    public static synchronized List<String> componentIds() {
        List<String> ids = new ArrayList<>();
        for (TextGlComponent component : COMPONENTS) ids.add(component.id());
        return java.util.Collections.unmodifiableList(ids);
    }

    /** Immutable component state for UI/diagnostics consumers. */
    public static synchronized List<ComponentInfo> componentInfos() {
        List<ComponentInfo> result = new ArrayList<>();
        for (TextGlComponent component : COMPONENTS) {
            boolean available = false;
            String status;
            try {
                available = component.isAvailable();
                status = component.status();
            } catch (RuntimeException | LinkageError error) {
                status = "error";
            }
            result.add(new ComponentInfo(component.id(), component.priority(), available, status));
        }
        return java.util.Collections.unmodifiableList(result);
    }

    public static final class ComponentInfo {
        public final String id;
        public final int priority;
        public final boolean available;
        public final String status;

        private ComponentInfo(String id, int priority, boolean available, String status) {
            this.id = id;
            this.priority = priority;
            this.available = available;
            this.status = status;
        }
    }
}
