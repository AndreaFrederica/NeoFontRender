package speiger.src.salutation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import carbonconfiglib.CarbonConfig;
import carbonconfiglib.config.Config;
import carbonconfiglib.config.ConfigEntry.BoolValue;
import carbonconfiglib.config.ConfigHandler;
import carbonconfiglib.config.ConfigSection;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import speiger.src.salutation.client.ClientHandler;

@Mod(modid = "salutation", version = "1.0.0", name = "Salutation", acceptableRemoteVersions = "*", dependencies = "required-after:carbonconfig")
public class Salutation {
	public static final Logger LOGGER = LogManager.getLogger("Salutation");
	public static BoolValue FORCE_SERVER_TRANSLATIONS;
	public static BoolValue DISABLE_OVERRIDE;
	public static ConfigHandler CONFIG;
	
	@Mod.EventHandler
	public void load(FMLPreInitializationEvent event) {
		Config config = new Config("Salutation");
		ConfigSection client = config.add("client");
		DISABLE_OVERRIDE = client.addBool("disable-chat-override", false);
		ConfigSection server = config.add("server");
		FORCE_SERVER_TRANSLATIONS = server.addBool("force-server-translations", false);
		CONFIG = CarbonConfig.CONFIGS.createConfig(config);
		CONFIG.register();
	}
	
	
	@Mod.EventHandler
	public void onLoad(FMLInitializationEvent event) {
		if(FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			initChatLoader();
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void initChatLoader() {
		ClientHandler.INSTANCE.init();
	}
}
