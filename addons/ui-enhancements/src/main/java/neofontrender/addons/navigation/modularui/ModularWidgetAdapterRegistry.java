package neofontrender.addons.navigation.modularui;

import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.widget.IWidget;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ModularWidgetAdapterRegistry {
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private ModularWidgetAdapterRegistry() {}

    public static synchronized UiRegistration register(ResourceLocation id, int priority,
                                                       ModularWidgetAdapter adapter) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(adapter, "adapter");
        for (Entry entry : ENTRIES) if (entry.id.equals(id)) {
            throw new IllegalArgumentException("duplicate ModularUI widget adapter: " + id);
        }
        Entry entry = new Entry(id, priority, adapter);
        ENTRIES.add(entry);
        ENTRIES.sort(Comparator.comparingInt((Entry value) -> value.priority).reversed()
                .thenComparing(value -> value.id.toString()));
        return () -> remove(entry);
    }

    static synchronized NavigationInfo resolve(IWidget widget, NavigationInfo declared) {
        for (Entry entry : ENTRIES) {
            if (entry.adapter.supports(widget)) {
                NavigationInfo adapted = entry.adapter.navigationInfo(widget, declared);
                return adapted == null ? declared : adapted;
            }
        }
        return declared;
    }

    private static synchronized void remove(Entry entry) { ENTRIES.remove(entry); }

    private static final class Entry {
        private final ResourceLocation id;
        private final int priority;
        private final ModularWidgetAdapter adapter;

        private Entry(ResourceLocation id, int priority, ModularWidgetAdapter adapter) {
            this.id = id;
            this.priority = priority;
            this.adapter = adapter;
        }
    }
}
