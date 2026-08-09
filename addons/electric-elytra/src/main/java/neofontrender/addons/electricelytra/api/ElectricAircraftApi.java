package neofontrender.addons.electricelytra.api;

import net.minecraft.entity.EntityLivingBase;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.electricelytra.compat.IEnergyStorage;
import neofontrender.addons.electricelytra.ElectricElytraConfig;
import neofontrender.addons.electricelytra.ItemElectricElytra;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Public discovery and engine-telemetry registry for third-party powered aircraft. */
public final class ElectricAircraftApi {
    public static final int API_VERSION = 1;
    private static final List<Entry> PROVIDERS = new ArrayList<>();

    private ElectricAircraftApi() {}

    public static synchronized ElectricAircraftRegistration register(
            ResourceLocation id, int priority, ElectricAircraftProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        Entry entry = new Entry(id.toString(), priority, provider);
        PROVIDERS.removeIf(value -> value.id.equals(entry.id));
        PROVIDERS.add(entry);
        PROVIDERS.sort(Comparator.comparingInt((Entry value) -> value.priority)
                .reversed().thenComparing(value -> value.id));
        return () -> unregister(entry);
    }

    @Nullable public static ElectricAircraft query(EntityLivingBase entity) {
        if (entity == null) return null;
        List<Entry> snapshot;
        synchronized (ElectricAircraftApi.class) { snapshot = new ArrayList<>(PROVIDERS); }
        for (Entry entry : snapshot) {
            try {
                ElectricAircraft aircraft = entry.provider.findAircraft(entity);
                if (aircraft != null) return aircraft;
            } catch (RuntimeException ignored) {
                // A faulty optional provider must not break other aircraft or vanilla travel.
            }
        }
        return builtIn(entity);
    }

    private static synchronized void unregister(Entry entry) { PROVIDERS.remove(entry); }

    @Nullable private static ElectricAircraft builtIn(EntityLivingBase entity) {
        ItemStack stack = EntityEquipmentSlot.getChest(entity);
        if (!ItemElectricElytra.isElectricElytra(stack)) return null;
        return new ElectricAircraft() {
            @Override public ElectricFlightModel getFlightModel() {
                return ItemElectricElytra.usesVanillaFlightModel(stack)
                        ? ElectricFlightModel.VANILLA_ELYTRA : ElectricFlightModel.AERODYNAMIC;
            }

            @Override public boolean isEngineEnabled() {
                return ItemElectricElytra.isEngineEnabled(stack);
            }

            @Override public boolean hasEnergy() {
                IEnergyStorage energy = ItemElectricElytra.getEnergy(stack);
                return energy != null && energy.getEnergyStored() > 0;
            }

            @Override public double getThrottleFraction() {
                return ItemElectricElytra.getThrottle(stack) / 100.0D;
            }

            @Override public double getMaximumThrustAcceleration(EntityLivingBase ignored) {
                return ElectricElytraConfig.maximumThrustAcceleration;
            }

            @Override public double getSpeedLimitBlocksPerSecond(EntityLivingBase ignored) {
                return ElectricElytraConfig.hardSpeedLimitBlocksPerSecond;
            }
        };
    }

    private static final class Entry {
        final String id;
        final int priority;
        final ElectricAircraftProvider provider;

        private Entry(String id, int priority, ElectricAircraftProvider provider) {
            this.id = id;
            this.priority = priority;
            this.provider = provider;
        }
    }
}
