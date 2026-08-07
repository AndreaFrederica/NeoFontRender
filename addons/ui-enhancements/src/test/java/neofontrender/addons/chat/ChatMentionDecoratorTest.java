package neofontrender.addons.chat;

import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatMentionDecoratorTest {
    @Test
    void colorsOnlineMentionsAndAddsHoverText() {
        Map<String, String> players = new LinkedHashMap<>();
        players.put("alice", "Alice");
        players.put("bob", "Bob");
        IChatComponent result = ChatMentionDecorator.decorate(
                new ChatComponentText("hello @Alice and @Bob"), players, "Alice", "Player: %s");

        IChatComponent alice = result.getSiblings().get(1);
        IChatComponent bob = result.getSiblings().get(3);
        assertEquals(EnumChatFormatting.GOLD, alice.getChatStyle().getColor());
        assertEquals(EnumChatFormatting.AQUA, bob.getChatStyle().getColor());
        HoverEvent hover = alice.getChatStyle().getChatHoverEvent();
        assertNotNull(hover);
        assertEquals("Player: Alice", hover.getValue().getUnformattedText());
    }
}
