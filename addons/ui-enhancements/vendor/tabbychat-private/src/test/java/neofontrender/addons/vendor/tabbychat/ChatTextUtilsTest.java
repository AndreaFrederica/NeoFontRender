package neofontrender.addons.vendor.tabbychat.test;



import neofontrender.addons.vendor.tabbychat.util.ChatTextUtils;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.ClickEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatTextUtilsTest {

    @Test
    public void testSubChat() {
        assertEquals(makeChat(false), ChatTextUtils.subChat(makeChat(true), 7));
    }

    private static IChatComponent makeChat(boolean tag) {

        IChatComponent chat = new ChatComponentText(tag ? "[test] " : "");
        chat.getChatStyle().setBold(true);
        {
            IChatComponent colored = new ChatComponentText("This should be green. ");
            colored.getChatStyle().setColor(EnumChatFormatting.GREEN);
            chat.appendSibling(colored);
        }
        chat.appendText(" ");
        {
            IChatComponent link = new ChatComponentText("This is a link.");
            link.getChatStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://google.com/"));
            chat.appendSibling(link);
        }
        return chat;
    }
}
