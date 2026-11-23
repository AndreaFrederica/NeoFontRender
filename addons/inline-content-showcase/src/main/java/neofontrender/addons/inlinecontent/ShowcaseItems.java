package neofontrender.addons.inlinecontent;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = InlineContentShowcaseMod.MOD_ID)
public final class ShowcaseItems {
    public static final CreativeTabs TAB = new CreativeTabs(InlineContentShowcaseMod.MOD_ID) {
        @Override public ItemStack createIcon() {
            return new ItemStack(TYPST_CHEMISTRY_DEMONSTRATOR);
        }
    };
    public static final Item AMINOBENZO_CROWN_ETHER_SOLUTION =
            new ItemCrownEtherSolution("aminobenzo_18_crown_6_solution");
    public static final Item TYPST_CHEMISTRY_DEMONSTRATOR =
            new ItemTypstChemistry("typst_chemistry_demonstrator");
    public static final Item PERIODIC_TABLE = new ItemTypstSample(null);
    public static final java.util.List<Item> ALL = createItems();

    private static java.util.List<Item> createItems() {
        java.util.ArrayList<Item> items = new java.util.ArrayList<>();
        items.add(AMINOBENZO_CROWN_ETHER_SOLUTION);
        items.add(TYPST_CHEMISTRY_DEMONSTRATOR);
        items.add(PERIODIC_TABLE);
        for (var element : TypstElementCatalog.ALL) items.add(new ItemTypstElement(element));
        for (var chemical : TypstChemicalCatalog.ALL) items.add(new ItemTypstSample(chemical));
        return java.util.List.copyOf(items);
    }

    private ShowcaseItems() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ALL.toArray(new Item[0]));
    }
}
