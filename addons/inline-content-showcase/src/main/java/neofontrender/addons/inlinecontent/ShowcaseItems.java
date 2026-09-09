package neofontrender.addons.inlinecontent;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = InlineContentShowcaseMod.MOD_ID)
public final class ShowcaseItems {
    public static final Item AMINOBENZO_CROWN_ETHER_SOLUTION =
            new ItemCrownEtherSolution("aminobenzo_18_crown_6_solution");
    public static final Item TYPST_CHEMISTRY_DEMONSTRATOR =
            new ItemTypstChemistry("typst_chemistry_demonstrator");

    private ShowcaseItems() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(AMINOBENZO_CROWN_ETHER_SOLUTION);
        event.getRegistry().register(TYPST_CHEMISTRY_DEMONSTRATOR);
    }
}
