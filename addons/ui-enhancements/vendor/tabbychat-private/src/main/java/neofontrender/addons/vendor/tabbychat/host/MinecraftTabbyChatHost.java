package neofontrender.addons.vendor.tabbychat.host;

import neofontrender.addons.mixin.tabbychat.IGuiIngame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;

import java.io.File;

/** Default host implementation for the Minecraft 1.7.10 client. */
public final class MinecraftTabbyChatHost implements TabbyChatHost {

    private final Minecraft minecraft;

    public MinecraftTabbyChatHost(Minecraft minecraft) {
        if (minecraft == null) {
            throw new IllegalArgumentException("minecraft");
        }
        this.minecraft = minecraft;
    }

    @Override
    public File getGameDirectory() {
        return minecraft.mcDataDir;
    }

    @Override
    public void registerEventHandler(Object listener) {
        MinecraftForge.EVENT_BUS.register(listener);
    }

    @Override
    public void registerResourceReloadListener(IResourceManagerReloadListener listener) {
        IReloadableResourceManager resources = (IReloadableResourceManager) minecraft.getResourceManager();
        resources.registerReloadListener(listener);
    }

    @Override
    public void installChatGui(GuiNewChat chatGui) {
        ((IGuiIngame) minecraft.ingameGUI).setPersistantChatGUI(chatGui);
    }

    @Override
    public void displayGuiScreen(GuiScreen screen) {
        minecraft.displayGuiScreen(screen);
    }
}
