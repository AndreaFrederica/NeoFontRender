package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;
import speiger.src.salutation.common.commands.CommandBuilder;
import speiger.src.salutation.common.commands.CommandNode;
import speiger.src.salutation.common.commands.args.IntegerArgument;
import speiger.src.salutation.common.commands.args.StringWalker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests for the complete embedded Salutation command tree implementation. */
class SalutationCommandParityTest {
    @Test
    void parsesLiteralAndIntegerArgumentsWithOriginalNodeSemantics() {
        CommandNode root = new CommandBuilder("uie")
                .literal("limit")
                .arg("value", IntegerArgument.range(1, 64))
                .build();
        CommandContext context = new CommandContext(null);
        CommandNode leaf = root.findCurrentNode(new StringWalker(new String[] {"limit", "42"}), context);

        assertNotNull(leaf);
        assertTrue(leaf.getCommand() == null);
        assertEquals(42, context.getArgument("value", Integer.class));
    }
}
