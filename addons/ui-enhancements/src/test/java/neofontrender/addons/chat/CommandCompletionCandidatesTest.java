package neofontrender.addons.chat;

import net.minecraft.util.text.TextFormatting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandCompletionCandidatesTest {
    @Test
    void mergesClientBeforeServerAndRemovesForgeFormatting() {
        String[] client = {TextFormatting.GRAY + "/neofontrender" + TextFormatting.RESET};
        String[] server = {"/help", "/thaumcraft"};

        CommandCompletionCandidates.Merge merged =
                CommandCompletionCandidates.merge(server, client);

        assertArrayEquals(new String[]{"/neofontrender", "/help", "/thaumcraft"},
                merged.plainValues());
        List<String> styled = merged.styledValues();
        assertEquals(TextFormatting.GOLD + "/neofontrender" + TextFormatting.RESET,
                styled.get(0));
        assertEquals(TextFormatting.AQUA + "/help" + TextFormatting.RESET, styled.get(1));
    }

    @Test
    void clientSourceWinsWhenForgeWouldExecuteTheClientCommand() {
        String[] client = {TextFormatting.GRAY + "/shared" + TextFormatting.RESET};
        String[] server = {"/shared", "/server-only"};

        CommandCompletionCandidates.Merge merged =
                CommandCompletionCandidates.merge(server, client);

        assertArrayEquals(new String[]{"/shared", "/server-only"}, merged.plainValues());
        assertEquals(CommandCompletionCandidates.Source.CLIENT, merged.sourceOf("shared"));
        assertEquals(CommandCompletionCandidates.Source.SERVER,
                merged.sourceOf("/server-only"));
    }

    @Test
    void displayFormattingNeverLeaksIntoInsertedValue() {
        String styled = CommandCompletionCandidates.styled(
                "/thaumcraft", CommandCompletionCandidates.Source.SERVER);

        assertEquals("/thaumcraft", CommandCompletionCandidates.plain(styled));
    }
}
