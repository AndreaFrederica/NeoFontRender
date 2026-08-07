package neofontrender.addons.electricelytra;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ElectricElytraMod.MOD_ID)
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

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(VANILLA_ELECTRIC_ELYTRA,
                CREATIVE_VANILLA_ELECTRIC_ELYTRA, ELECTRIC_ELYTRA,
                ADVANCED_ELECTRIC_ELYTRA,
                ADVANCED_FLAP_ELECTRIC_ELYTRA, CREATIVE_ELECTRIC_ELYTRA);
    }
}
