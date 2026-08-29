package neofontrender.addons.api.command.server;

import neofontrender.addons.api.command.CommandCompletionResult;

/**
 * Supplies authoritative server candidates for commands with an incomplete completion contract.
 * Providers run synchronously on the server thread and must not perform blocking I/O.
 */
@FunctionalInterface
public interface ServerCommandCompletionProvider {
    CommandCompletionResult complete(ServerCommandCompletionContext context);
}
