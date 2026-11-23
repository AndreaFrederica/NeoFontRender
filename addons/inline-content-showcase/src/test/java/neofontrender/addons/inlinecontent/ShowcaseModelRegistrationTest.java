package neofontrender.addons.inlinecontent;

import neofontrender.addons.electricelytra.ElectricElytraItems;
import neofontrender.addons.electricelytra.client.ClientProxy;
import neofontrender.addons.inlinecontent.client.InlineContentShowcaseClient;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IRegistryDelegate;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShowcaseModelRegistrationTest {
    @Test void showcaseAndElytraKeepIndependentModelsAfterItemRegistration() throws Exception {
        Bootstrap.register();
        var registryEvent = new RegistryEvent.Register<Item>(new ResourceLocation("minecraft:item"), ForgeRegistries.ITEMS);
        ElectricElytraItems.registerItems(registryEvent);
        ShowcaseItems.registerItems(registryEvent);
        ClientProxy.registerModels(new ModelRegistryEvent());
        InlineContentShowcaseClient.registerModels(new ModelRegistryEvent());

        var field = ModelLoader.class.getDeclaredField("customModels");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        var models = (Map<Pair<IRegistryDelegate<Item>, Integer>, ModelResourceLocation>) field.get(null);
        for (Item item : ShowcaseItems.ALL) assertModel(models, item);
        for (Item item : new Item[]{ElectricElytraItems.VANILLA_ELECTRIC_ELYTRA,
                ElectricElytraItems.CREATIVE_VANILLA_ELECTRIC_ELYTRA, ElectricElytraItems.ELECTRIC_ELYTRA,
                ElectricElytraItems.ADVANCED_ELECTRIC_ELYTRA, ElectricElytraItems.ADVANCED_FLAP_ELECTRIC_ELYTRA,
                ElectricElytraItems.CREATIVE_ELECTRIC_ELYTRA}) assertModel(models, item);
    }

    private static void assertModel(Map<Pair<IRegistryDelegate<Item>, Integer>, ModelResourceLocation> models, Item item) {
        assertEquals(item.getRegistryName(), item.delegate.name());
        assertEquals(new ModelResourceLocation(item.getRegistryName(), "inventory"), models.get(Pair.of(item.delegate, 0)),
                item.getRegistryName().toString());
    }
}
