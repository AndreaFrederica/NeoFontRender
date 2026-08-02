package speiger.src.salutation.client;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSleepMP;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import speiger.src.salutation.Salutation;
import speiger.src.salutation.client.gui.chat.ChatScreen;
import speiger.src.salutation.client.gui.chat.ISaluationChat;
import speiger.src.salutation.client.gui.chat.MPChatScreen;
import speiger.src.salutation.client.gui.chat.MultilineChatScreen;
import neofontrender.addons.chat.EnhancedChatConfigAccess;

@SideOnly(Side.CLIENT)
public class ClientHandler {
	public static final ClientHandler INSTANCE = new ClientHandler();
	boolean replacedChat = false;
	
	public void init() {
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event) {
		GuiScreen screen = event.getGui();
		Minecraft mc = Minecraft.getMinecraft();
		//TODO Decide if chunk pregen gets a dependency on this.
		//if not then one of the two need to yield. At the moment it will be salutation.
		if(Loader.isModLoaded("chunkpregenerator")) return; 
		// The embedded build shares UIE's master switch and TabbyChat toggle. This
		// keeps the original Salutation path available only when the enhanced renderer
		// is disabled, without changing the upstream screen behavior itself.
		boolean disable = Salutation.DISABLE_OVERRIDE.get()
				|| EnhancedChatConfigAccess.tabbedChatEnabled()
				|| !EnhancedChatConfigAccess.chatEnabled();
		if(screen instanceof GuiMainMenu) {
			if(!replacedChat && !disable) {
				ReflectionHelper.setPrivateValue(GuiIngame.class, mc.ingameGUI, new MultilineChatScreen(), "persistantChatGUI", "field_73840_e");
				replacedChat = true;
			}
			else if(replacedChat && disable) {
				ReflectionHelper.setPrivateValue(GuiIngame.class, mc.ingameGUI, new GuiNewChat(mc), "persistantChatGUI", "field_73840_e");
				replacedChat = false;
			}
		}
		else if(!disable && screen instanceof GuiChat && !(screen instanceof ISaluationChat)) {
			if(screen instanceof GuiSleepMP) {
				event.setCanceled(true);
				mc.displayGuiScreen(new MPChatScreen());
			}
			else {
				event.setCanceled(true);
				mc.displayGuiScreen(new ChatScreen((GuiChat)screen));
			}
		}
	}
}
