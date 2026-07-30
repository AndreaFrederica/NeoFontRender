package neofontrender.addons.chat;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.host.MinecraftTabbyChatHost;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

/** Activates vanilla chat history, persistence, and smooth scrolling controls. */
public final class EnhancedChatModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        EnhancedChatConfig.load();
        ChatStyleConfig.load();
        ChatHistoryManager.INSTANCE.initialize();
    }

    @Override
    public void init() {
        // The vendored TabbyChat must be started before any chat screen can open: MixinGuiChat
        // resolves TabbyChat.getInstance() while initialising. Startup creates GuiText fields,
        // so it must run at init when Minecraft.fontRenderer already exists (preInit is earlier).
        // An external TabbyChat mod owns the chat instead and keeps the embedded backend disabled.
        if (!ExternalChatCompat.tabbyChatLoaded() && !TabbyChat.isStarted()) {
            TabbyChat.start(new MinecraftTabbyChatHost(Minecraft.getMinecraft()));
        }
        NfrSettingsPageRegistry.register(new EnhancedChatSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new TabbedChatSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new ChatStyleSettingsPage());
        FMLCommonHandler.instance().bus().register(ChatHistoryManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatCopyController.INSTANCE);
        ChatKeyBindings.register();
    }
}
