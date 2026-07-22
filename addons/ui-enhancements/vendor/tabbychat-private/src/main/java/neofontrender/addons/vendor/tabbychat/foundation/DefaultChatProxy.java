package neofontrender.addons.vendor.tabbychat.foundation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentTranslation;

public class DefaultChatProxy implements IChatProxy {

    @Override
    public void addToChat(String channel, IChatComponent msg) {
        IChatComponent text = new ChatComponentTranslation("[%s] %s", channel, msg);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(text);
    }
}
