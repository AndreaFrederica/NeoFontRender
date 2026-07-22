package neofontrender.addons.vendor.tabbychat.host;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.IResourceManagerReloadListener;

import java.io.File;

/**
 * Provides the Minecraft lifecycle operations that an embedding addon must own.
 * This keeps the vendored core independent from a second FML mod container.
 */
public interface TabbyChatHost {

    /** Returns the directory under which the private TabbyChat data is stored. */
    File getGameDirectory();

    /** Registers a listener on the Forge event bus. */
    void registerEventHandler(Object listener);

    /** Registers a listener that follows Minecraft resource reloads. */
    void registerResourceReloadListener(IResourceManagerReloadListener listener);

    /** Replaces the vanilla in-game chat GUI with the vendored implementation. */
    void installChatGui(GuiNewChat chatGui);

    /** Opens a screen through the active Minecraft client. */
    void displayGuiScreen(GuiScreen screen);
}
