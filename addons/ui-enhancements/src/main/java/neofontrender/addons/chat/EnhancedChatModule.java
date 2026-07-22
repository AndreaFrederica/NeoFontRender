package neofontrender.addons.chat;

import mnm.mods.tabbychat.TabbyChat;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;

public final class EnhancedChatModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        EnhancedChatConfig.load();
        ChatStyleConfig.load();
        ChatHistoryManager.INSTANCE.initialize();
        if (!ExternalChatCompat.tabbyChatLoaded()) TabbyChat.getInstance().init();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new EnhancedChatSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new TabbedChatSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new ChatStyleSettingsPage());
        MinecraftForge.EVENT_BUS.register(ChatHistoryManager.INSTANCE);
        if (!ExternalChatCompat.tabbyChatLoaded()) TabbyChat.getInstance().postInit();
    }
}
