package neofontrender.addons.api.flight.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Dedicated/integrated server policy API for UIE flight orientation synchronization. */
public final class FlightServerApi {
    public static final int API_VERSION = 1;
    private static final CopyOnWriteArrayList<Entry> PROVIDERS = new CopyOnWriteArrayList<>();
    private static volatile FlightServerPolicy defaults =
            new FlightServerPolicy(true, true, true, 180.0F, 192.0D);

    private FlightServerApi() {}

    public static FlightServerRegistration registerPolicyProvider(ResourceLocation id, int priority,
                                                                  FlightServerPolicyProvider provider) {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(provider, "provider");
        String key = id.toString();
        PROVIDERS.removeIf(value -> value.id.equals(key));
        Entry entry = new Entry(key, priority, provider);
        PROVIDERS.add(entry);
        PROVIDERS.sort(Comparator.comparingInt((Entry value) -> value.priority).reversed()
                .thenComparing(value -> value.id));
        return new FlightServerRegistration() {
            private boolean closed;
            @Override public synchronized void close() {
                if (!closed) { closed = true; PROVIDERS.remove(entry); }
            }
        };
    }

    public static FlightServerPolicy getDefaultPolicy() { return defaults; }

    public static void configureDefaults(boolean enabled, boolean synchronizationEnabled,
                                         float maximumRollSpeed) {
        FlightServerPolicy old = defaults;
        defaults = new FlightServerPolicy(enabled, synchronizationEnabled,
                old.isElytraRequired(), maximumRollSpeed, old.getSynchronizationRange());
    }

    public static FlightServerPolicy policyFor(EntityPlayerMP player) {
        FlightServerPolicy result = defaults;
        for (Entry entry : PROVIDERS) {
            try {
                FlightServerPolicy changed = entry.provider.apply(player, result);
                if (changed != null) result = changed;
            } catch (RuntimeException ignored) { }
        }
        return result;
    }

    private static final class Entry {
        final String id; final int priority; final FlightServerPolicyProvider provider;
        Entry(String id, int priority, FlightServerPolicyProvider provider) {
            this.id = id; this.priority = priority; this.provider = provider;
        }
    }
}
