package neofontrender.api.client.settings;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void laboratoryContributionsAreIsolatedFromFixes() {
        String id = "registry_test:laboratory";
        try {
            NfrSettingsSectionRegistry.register(contribution(
                    id, 10, NfrSettingsSection.LABORATORY));

            assertTrue(NfrSettingsSectionRegistry.snapshot(NfrSettingsSection.FIXES).isEmpty());
            assertEquals(id, NfrSettingsSectionRegistry
                    .snapshot(NfrSettingsSection.LABORATORY).get(0).id());
        } finally {
            assertTrue(NfrSettingsSectionRegistry.unregister(id));
        }
    }

    private static NfrSettingsSectionContribution contribution(String id, int order) {
        return contribution(id, order, NfrSettingsSection.FIXES);
    }

    private static NfrSettingsSectionContribution contribution(
            String id, int order, NfrSettingsSection section) {
        return new NfrSettingsSectionContribution() {
            @Override public String id() { return id; }
            @Override public NfrSettingsSection section() { return section; }
            @Override public int order() { return order; }
            @Override public NfrSettingsSectionSession createSession() {
                return context -> Collections.emptyList();
            }
        };
    }
}
