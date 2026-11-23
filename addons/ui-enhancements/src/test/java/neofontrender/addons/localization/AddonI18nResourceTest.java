package neofontrender.addons.localization;

import net.minecraft.client.resources.IResource;
import neofontrender.addons.tooltips.AddonI18n;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonI18nResourceTest {
    @Test
    void resourcesCarryTheAddonDomainAndPreserveLanguageOrder() throws Exception {
        List<IResource> resources = AddonI18n.bundledLanguageResources(
                Arrays.asList("en_us", "zh_cn"));

        assertEquals(2, resources.size());
        assertEquals(AddonI18n.DOMAIN + ":lang/en_us.lang",
                resources.get(0).getResourceLocation().toString());
        assertEquals(AddonI18n.DOMAIN + ":lang/zh_cn.lang",
                resources.get(1).getResourceLocation().toString());
        try (InputStream stream = resources.get(0).getInputStream()) {
            String contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(contents.contains("neofontrender_ui_enhancements.info.version="));
        }
    }

    @Test
    void englishFallbackIsAddedWhenTheLanguageListOmitsIt() {
        List<IResource> resources = AddonI18n.bundledLanguageResources(
                Collections.singletonList("zh_cn"));

        assertEquals(2, resources.size());
        assertEquals(AddonI18n.DOMAIN + ":lang/en_us.lang",
                resources.get(0).getResourceLocation().toString());
        assertEquals(AddonI18n.DOMAIN + ":lang/zh_cn.lang",
                resources.get(1).getResourceLocation().toString());
    }
}
