package neofontrender.addons.chat;

import mnm.mods.tabbychat.TabbyChat;
import net.minecraftforge.common.MinecraftForge;
import neofontrender.addons.ui.UiEnhancementModule;
import neofontrender.api.client.settings.NfrSettingsPageRegistry;
import neofontrender.addons.chat.network.SelfMessageCapability;
import speiger.src.salutation.Salutation;
import speiger.src.salutation.client.ClientHandler;

public final class EnhancedChatModule implements UiEnhancementModule {
    @Override
    public void preInit() {
        SelfMessageCapability.initialize();
        EnhancedChatConfig.load();
        ChatStyleConfig.load();
        Salutation.initialize();
        if (!ExternalChatCompat.salutationLoaded()) ClientHandler.INSTANCE.init();
        ChatHistoryManager.INSTANCE.initialize();
        if (!ExternalChatCompat.tabbyChatLoaded()) TabbyChat.getInstance().init();
    }

    @Override
    public void init() {
        NfrSettingsPageRegistry.register(new EnhancedChatSettingsPage());
        NfrSettingsPageRegistry.register(new ChatRulesSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new TabbedChatSettingsPage());
        if (!ExternalChatCompat.tabbyChatLoaded()) NfrSettingsPageRegistry.register(new ChatStyleSettingsPage());
        MinecraftForge.EVENT_BUS.register(ChatHistoryManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatMessageProcessor.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatCopyController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatSearchController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(MentionCompletionController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatCommandCompletionController.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ChatHudWindowController.INSTANCE);
        ChatKeyBindings.register();
        if (!ExternalChatCompat.tabbyChatLoaded()) TabbyChat.getInstance().postInit();
    }
}
