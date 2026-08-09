package neofontrender.addons.electricelytra;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public final class ElectricElytraItems {
    /** Vanilla steering/glide physics with only the shared FE engine layered on top. */
    public static final ItemElectricElytra VANILLA_ELECTRIC_ELYTRA =
            new ItemElectricElytra("vanilla_electric_elytra", false, false, false, true);
    /** Creative-only vanilla-flight counterpart with inexhaustible FE. */
    public static final ItemElectricElytra CREATIVE_VANILLA_ELECTRIC_ELYTRA =
            new ItemElectricElytra("creative_vanilla_electric_elytra",
                    false, false, true, true);
    public static final ItemElectricElytra ELECTRIC_ELYTRA =
            new ItemElectricElytra("electric_elytra", false, false);
    public static final ItemElectricElytra ADVANCED_ELECTRIC_ELYTRA =
            new ItemElectricElytra("advanced_electric_elytra", true, false);
    public static final ItemElectricElytra ADVANCED_FLAP_ELECTRIC_ELYTRA =
            new ItemElectricElytra("advanced_flap_electric_elytra", true, true);
    /** Creative-only reference aircraft: maximum feature set and inexhaustible FE. */
    public static final ItemElectricElytra CREATIVE_ELECTRIC_ELYTRA =
            new ItemElectricElytra("creative_electric_elytra", true, true, true);

    private ElectricElytraItems() {}

    public static void register() {
        GameRegistry.registerItem(VANILLA_ELECTRIC_ELYTRA, "vanilla_electric_elytra");
        GameRegistry.registerItem(CREATIVE_VANILLA_ELECTRIC_ELYTRA, "creative_vanilla_electric_elytra");
        GameRegistry.registerItem(ELECTRIC_ELYTRA, "electric_elytra");
        GameRegistry.registerItem(ADVANCED_ELECTRIC_ELYTRA, "advanced_electric_elytra");
        GameRegistry.registerItem(ADVANCED_FLAP_ELECTRIC_ELYTRA, "advanced_flap_electric_elytra");
        GameRegistry.registerItem(CREATIVE_ELECTRIC_ELYTRA, "creative_electric_elytra");

        GameRegistry.addRecipe(new ItemStack(ELECTRIC_ELYTRA),
                "RDR", "IEI", "RBR",
                'R', Blocks.redstone_block, 'D', Items.diamond,
                'I', Items.iron_ingot, 'E', Items.feather, 'B', Items.blaze_rod);
        GameRegistry.addRecipe(new ItemStack(VANILLA_ELECTRIC_ELYTRA),
                "RIR", "IEI", "RBR",
                'R', Blocks.redstone_block, 'I', Items.iron_ingot,
                'E', Items.feather, 'B', Items.blaze_rod);
        GameRegistry.addRecipe(new ItemStack(ADVANCED_ELECTRIC_ELYTRA),
                "CRC", "RER", "CRC",
                'C', Items.comparator, 'R', Blocks.redstone_block,
                'E', new ItemStack(ELECTRIC_ELYTRA));
        GameRegistry.addRecipe(new ItemStack(ADVANCED_FLAP_ELECTRIC_ELYTRA),
                "IPI", "RAR", "IPI",
                'I', Items.iron_ingot, 'P', Blocks.piston, 'R', Blocks.redstone_block,
                'A', new ItemStack(ADVANCED_ELECTRIC_ELYTRA));
    }
}
