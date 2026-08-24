package neofontrender.addons.navigation;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.ui.navigation.UiAction;
import neofontrender.addons.api.ui.navigation.UiActionResult;
import neofontrender.addons.api.ui.navigation.UiRegistration;
import neofontrender.addons.api.ui.navigation.UiTreeProvider;
import neofontrender.addons.api.ui.navigation.UiTreeSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiNavigationRegistryTest {
    @Test void selectsHighestPriorityThenLexicographicallySmallestId() {
        UiNavigationRegistry registry = new UiNavigationRegistry();
        GuiScreen screen = new GuiScreen() {};
        TestProvider low = new TestProvider();
        TestProvider z = new TestProvider();
        TestProvider a = new TestProvider();
        registry.register(new ResourceLocation("test", "low"), 0, low);
        registry.register(new ResourceLocation("test", "z"), 10, z);
        registry.register(new ResourceLocation("test", "a"), 10, a);

        assertEquals(a, registry.select(screen).provider());
    }

    @Test void registrationIsIdempotentlyRemovableAndIdsAreUnique() {
        UiNavigationRegistry registry = new UiNavigationRegistry();
        ResourceLocation id = new ResourceLocation("test", "provider");
        UiRegistration registration = registry.register(id, 0, new TestProvider());
        assertThrows(IllegalArgumentException.class, () -> registry.register(id, 1, new TestProvider()));
        registration.close();
        registration.close();
        assertEquals(0, registry.size());
    }

    private static final class TestProvider implements UiTreeProvider {
        @Override public boolean supports(GuiScreen screen) { return true; }
        @Override public UiTreeSession open(GuiScreen screen) { throw new UnsupportedOperationException(); }
    }
}
