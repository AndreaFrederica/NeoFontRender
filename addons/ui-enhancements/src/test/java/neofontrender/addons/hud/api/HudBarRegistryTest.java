package neofontrender.addons.hud.api;

import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HudBarRegistryTest {
    private final List<String> registeredIds = new ArrayList<String>();

    @AfterEach
    void unregisterTestProviders() {
        for (String id : registeredIds) HudBarRegistry.unregister(id);
    }

    @Test
    void rejectsDuplicateProviderIds() {
        register(new TestProvider("testhud:duplicate", 10));

        assertThrows(IllegalStateException.class,
                () -> HudBarRegistry.register(new TestProvider("testhud:duplicate", 20)));
    }

    @Test
    void ordersSnapshotByOrderThenStableId() {
        register(new TestProvider("testhud:zeta", 10));
        register(new TestProvider("testhud:later", 20));
        register(new TestProvider("testhud:alpha", 10));

        List<HudBarRegistration> providers = HudBarRegistry.snapshot(HudBarElement.HEALTH);

        assertEquals("testhud:alpha", providers.get(0).id());
        assertEquals("testhud:zeta", providers.get(1).id());
        assertEquals("testhud:later", providers.get(2).id());
    }

    @Test
    void rejectsUnnamespacedIds() {
        assertThrows(IllegalArgumentException.class,
                () -> HudBarRegistry.register(new TestProvider("missing_namespace", 10)));
    }

    private void register(HudBarProvider provider) {
        HudBarRegistry.register(provider);
        registeredIds.add(provider.id());
    }

    private static final class TestProvider implements HudBarProvider {
        private final String id;
        private final int order;

        private TestProvider(String id, int order) {
            this.id = id;
            this.order = order;
        }

        @Override public String id() { return id; }
        @Override public HudBarElement element() { return HudBarElement.HEALTH; }
        @Override public HudBarSide side() { return HudBarSide.LEFT; }
        @Override public int order() { return order; }
        @Override public boolean shouldRender(EntityPlayer player) { return true; }
        @Override public HudBarValue sample(EntityPlayer player, float partialTicks) {
            return new HudBarValue(1.0F, 1.0F, 0xFFFFFFFF, "");
        }
    }
}
