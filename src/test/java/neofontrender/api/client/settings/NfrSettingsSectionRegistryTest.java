package neofontrender.api.client.settings;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NfrSettingsSectionRegistryTest {
    @Test
    void contributionsAreNamespacedSortedAndRemovable() {
        String first = "registry_test:first";
        String second = "registry_test:second";
        try {
            NfrSettingsSectionRegistry.register(contribution(second, 20));
            NfrSettingsSectionRegistry.register(contribution(first, 10));

            assertEquals(first,
                    NfrSettingsSectionRegistry.snapshot(NfrSettingsSection.FIXES).get(0).id());
            assertThrows(IllegalStateException.class,
                    () -> NfrSettingsSectionRegistry.register(contribution(first, 30)));
        } finally {
            assertTrue(NfrSettingsSectionRegistry.unregister(first));
            assertTrue(NfrSettingsSectionRegistry.unregister(second));
        }
    }

    private static NfrSettingsSectionContribution contribution(String id, int order) {
        return new NfrSettingsSectionContribution() {
            @Override public String id() { return id; }
            @Override public NfrSettingsSection section() { return NfrSettingsSection.FIXES; }
            @Override public int order() { return order; }
            @Override public NfrSettingsSectionSession createSession() {
                return context -> Collections.emptyList();
            }
        };
    }
}
