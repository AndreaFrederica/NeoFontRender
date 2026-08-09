package neofontrender.addons.electricelytra;

import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.flight.server.FlightServerApi;
import neofontrender.addons.electricelytra.network.ElectricElytraNetwork;

@Mod(
        modid = ElectricElytraMod.MOD_ID,
        name = ElectricElytraMod.MOD_NAME,
        version = ElectricElytraMod.VERSION,
        dependencies = "required-after:neofontrender_ui_enhancements@[0.3.2,)",
        acceptedMinecraftVersions = "[1.7.10,1.8)"
)
public final class ElectricElytraMod {
    public static final String MOD_ID = "neofontrender_electric_elytra";
    public static final String MOD_NAME = "Revo Electric Elytra";
    public static final String VERSION = "0.1.0";

    @SidedProxy(
            clientSide = "neofontrender.addons.electricelytra.client.ClientProxy",
            serverSide = "neofontrender.addons.electricelytra.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ElectricElytraConfig.load();
        ElectricElytraItems.register();
        ElectricElytraNetwork.initialize();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FlightServerApi.registerPolicyProvider(new ResourceLocation(MOD_ID, "electric_elytra"),
                200, (player, current) -> ItemElectricElytra.isElectricElytra(
                        EntityEquipmentSlot.getChest(player))
                        ? current.withEnabled(true).withSynchronization(true)
                                .withElytraRequired(false).withMaximumRollSpeed(360.0F)
                                .withSynchronizationRange(384.0D)
                        : current);
        ElectricFlightController controller = new ElectricFlightController();
        MinecraftForge.EVENT_BUS.register(controller);
        FMLCommonHandler.instance().bus().register(controller);
        proxy.init();
    }
}
