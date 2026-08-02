package neofontrender.addons.chat;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
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
        ITextComponent result = ChatMentionDecorator.decorate(
                new TextComponentString("hello @Alice and @Bob"), players, "Alice", "Player: %s");

        ITextComponent alice = result.getSiblings().get(1);
        ITextComponent bob = result.getSiblings().get(3);
        assertEquals(TextFormatting.GOLD, alice.getStyle().getColor());
        assertEquals(TextFormatting.AQUA, bob.getStyle().getColor());
        HoverEvent hover = alice.getStyle().getHoverEvent();
        assertNotNull(hover);
        assertEquals("Player: Alice", hover.getValue().getUnformattedText());
    }
}
