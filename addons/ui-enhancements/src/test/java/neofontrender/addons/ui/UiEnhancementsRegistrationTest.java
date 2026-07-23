package neofontrender.addons.ui;

import neofontrender.addons.scrolling.SmoothScrollingModule;
import neofontrender.addons.tooltips.TooltipModule;
import neofontrender.api.client.settings.NfrInfoPage;
import neofontrender.api.client.settings.NfrInfoPageContribution;
import neofontrender.api.client.settings.NfrInfoPageRegistry;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiEnhancementsRegistrationTest {
    private static final List<String> PAGE_IDS = Arrays.asList(
            "neofontrender_ui_enhancements:scrolling",
            "neofontrender_ui_enhancements:tooltips");

    @AfterEach
    void removeRegistrations() {
        for (String id : PAGE_IDS) NfrSettingsPageRegistry.unregister(id);
        NfrInfoPageRegistry.unregister("neofontrender_ui_enhancements:about");
        NfrInfoPageRegistry.unregister("neofontrender_ui_enhancements:licenses");
    }

    @Test
    void modulesRegisterSettingsPagesWithoutAForgeRuntime() {
        new SmoothScrollingModule().init();
        new TooltipModule().init();

        Set<String> registered = NfrSettingsPageRegistry.snapshot().stream()
                .map(NfrSettingsPage::id)
                .filter(PAGE_IDS::contains)
                .collect(Collectors.toSet());

        assertEquals(new HashSet<>(PAGE_IDS), registered);
    }

    @Test
    void addonRegistersAboutAndLicenseContributions() {
        UiEnhancementsInfoContributions.register();

        assertEquals(
                Arrays.asList("neofontrender_ui_enhancements:about"),
                ids(NfrInfoPageRegistry.snapshot(NfrInfoPage.ABOUT)));
        assertEquals(
                Arrays.asList("neofontrender_ui_enhancements:licenses"),
                ids(NfrInfoPageRegistry.snapshot(NfrInfoPage.LICENSES)));
    }

    private static List<String> ids(List<NfrInfoPageContribution> contributions) {
        return contributions.stream().map(NfrInfoPageContribution::id).collect(Collectors.toList());
    }
}
