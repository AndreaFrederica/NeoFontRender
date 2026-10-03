package neofontrender.addons.inlinecontent;

import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.client.resources.Locale;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.client.resources.data.PackMetadataSection;
import net.minecraft.client.resources.data.PackMetadataSectionSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShowcaseGameResourceTest {
    @Test void installableJarLoadsTranslationsModelsAndTexturesThroughForge() throws Exception {
        ModMetadata metadata = new ModMetadata();
        metadata.modId = InlineContentShowcaseMod.MOD_ID;
        metadata.name = "Showcase regression";
        var container = new DummyModContainer(metadata) {
            @Override public File getResource() { return new File(System.getProperty("showcase.installableJar")); }
        };
        MetadataSerializer serializer = new MetadataSerializer();
        serializer.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
        try (FMLFileResourcePack raw = new FMLFileResourcePack(container)) {
            PackMetadataSection packMetadata = raw.getPackMetadata(serializer, "pack");
            // Follow FMLClientHandler.addModAsResource, including its format-2 language adapter.
            IResourcePack pack = packMetadata != null && packMetadata.getPackFormat() == 2
                    ? new LegacyV2Adapter(raw) : raw;
            var resources = new SimpleReloadableResourceManager(serializer);
            resources.reloadResources(List.of(pack));
            var locale = new Locale();
            locale.loadLocaleDataFiles(resources, List.of("en_us", "zh_cn"));
            assertEquals("NFR 渲染展示", locale.formatMessage("itemGroup." + metadata.modId, new Object[0]));
            assertEquals("镁 (Mg)", locale.formatMessage("item." + metadata.modId + ".element_magnesium.name", new Object[0]));
            assertTrue(locale.hasKey("showcase.typst.open"));

            var ids = new ArrayList<>(List.of("aminobenzo_18_crown_6_solution", "typst_chemistry_demonstrator", "periodic_table"));
            TypstChemicalCatalog.ALL.forEach(sample -> ids.add(sample.id()));
            TypstElementCatalog.ALL.forEach(element -> ids.add(element.id()));
            for (String id : ids) {
                assertTrue(locale.hasKey("item." + metadata.modId + "." + id + ".name"), id);
                try (var model = resources.getResource(new ResourceLocation(metadata.modId, "models/item/" + id + ".json"));
                     var reader = new InputStreamReader(model.getInputStream(), StandardCharsets.UTF_8)) {
                    var json = new JsonParser().parse(reader).getAsJsonObject();
                    assertEquals("item/generated", json.get("parent").getAsString());
                    var texture = new ResourceLocation(json.getAsJsonObject("textures").get("layer0").getAsString());
                    try (var resource = resources.getResource(new ResourceLocation(texture.getNamespace(), "textures/" + texture.getPath() + ".png"))) {
                        var image = ImageIO.read(resource.getInputStream());
                        assertNotNull(image, id);
                        assertEquals(32, image.getWidth(), id);
                        assertEquals(32, image.getHeight(), id);
                    }
                }
            }
        }
    }
}
