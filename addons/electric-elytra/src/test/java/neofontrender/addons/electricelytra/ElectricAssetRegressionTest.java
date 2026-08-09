package neofontrender.addons.electricelytra;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ElectricAssetRegressionTest {
    @Test
    void everyBuiltInAircraftExplicitlyUsesTheVanillaElytraIcon() throws Exception {
        for (String name : new String[]{"vanilla_electric_elytra",
                "creative_vanilla_electric_elytra", "electric_elytra",
                "advanced_electric_elytra", "advanced_flap_electric_elytra",
                "creative_electric_elytra"}) {
            String path = "/assets/neofontrender_electric_elytra/models/item/" + name + ".json";
            try (InputStream stream = ElectricAssetRegressionTest.class.getResourceAsStream(path)) {
                assertNotNull(stream, path);
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(json.contains("minecraft:items/elytra"), path);
            }
        }
    }
}
