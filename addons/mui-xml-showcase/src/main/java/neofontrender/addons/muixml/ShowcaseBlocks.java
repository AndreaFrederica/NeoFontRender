package neofontrender.addons.muixml;

import neofontrender.addons.muixml.block.ShowcaseMachineBlock;
import neofontrender.addons.muixml.tile.ShowcaseChestTile;
import neofontrender.addons.muixml.tile.ShowcaseFurnaceTile;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = MuiXmlShowcaseMod.MOD_ID)
public final class ShowcaseBlocks {

    public static final ShowcaseMachineBlock XML_CHEST = new ShowcaseMachineBlock(
            "xml_chest", ShowcaseMachineBlock.Kind.CHEST);
    public static final ShowcaseMachineBlock XML_FURNACE = new ShowcaseMachineBlock(
            "xml_furnace", ShowcaseMachineBlock.Kind.FURNACE);

    private ShowcaseBlocks() {}

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(ShowcaseChestTile.class,
                new ResourceLocation(MuiXmlShowcaseMod.MOD_ID, "xml_chest"));
        GameRegistry.registerTileEntity(ShowcaseFurnaceTile.class,
                new ResourceLocation(MuiXmlShowcaseMod.MOD_ID, "xml_furnace"));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(XML_CHEST, XML_FURNACE);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(itemBlock(XML_CHEST), itemBlock(XML_FURNACE));
    }

    private static ItemBlock itemBlock(Block block) {
        return (ItemBlock) new ItemBlock(block).setRegistryName(block.getRegistryName());
    }
}
