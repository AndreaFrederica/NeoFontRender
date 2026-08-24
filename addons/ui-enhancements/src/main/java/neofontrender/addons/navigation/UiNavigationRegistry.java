package neofontrender.addons.navigation;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiRegistration;
import neofontrender.addons.api.ui.navigation.UiTreeProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class UiNavigationRegistry {
    private static final UiNavigationRegistry GLOBAL = new UiNavigationRegistry();

    private final List<Entry> entries = new ArrayList<>();

    public static UiNavigationRegistry global() { return GLOBAL; }

    public synchronized UiRegistration register(ResourceLocation id, int priority, UiTreeProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        for (Entry entry : entries) {
            if (entry.id.equals(id)) throw new IllegalArgumentException("duplicate UI tree provider: " + id);
        }
        Entry entry = new Entry(id, priority, provider);
        entries.add(entry);
        entries.sort(Comparator.comparingInt(Entry::priority).reversed()
                .thenComparing(value -> value.id.toString()));
        return new Registration(this, entry);
    }

    public synchronized Selection select(GuiScreen screen) {
        Objects.requireNonNull(screen, "screen");
        for (Entry entry : entries) {
            if (entry.provider.supports(screen)) return new Selection(entry.id, entry.provider);
        }
        return null;
    }

    synchronized int size() { return entries.size(); }

    private synchronized void remove(Entry entry) { entries.remove(entry); }

    public static final class Selection {
        private final ResourceLocation id;
        private final UiTreeProvider provider;

        private Selection(ResourceLocation id, UiTreeProvider provider) {
            this.id = id;
            this.provider = provider;
        }

        public ResourceLocation id() { return id; }
        public UiTreeProvider provider() { return provider; }
    }

    private static final class Entry {
        private final ResourceLocation id;
        private final int priority;
        private final UiTreeProvider provider;

        private Entry(ResourceLocation id, int priority, UiTreeProvider provider) {
            this.id = id;
            this.priority = priority;
            this.provider = provider;
        }

        private int priority() { return priority; }
    }

    private static final class Registration implements UiRegistration {
        private UiNavigationRegistry registry;
        private Entry entry;

        private Registration(UiNavigationRegistry registry, Entry entry) {
            this.registry = registry;
            this.entry = entry;
        }

        @Override public synchronized void close() {
            if (entry == null) return;
            registry.remove(entry);
            registry = null;
            entry = null;
        }
    }
}
