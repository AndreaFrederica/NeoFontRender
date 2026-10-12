package neofontrender.addons.api.command.client;

import neofontrender.addons.api.command.CommandCompletionResult;

/** Supplies local candidates for client-side commands and client-only enhancements. */
@FunctionalInterface
public interface ClientCommandCompletionProvider {
    CommandCompletionResult complete(ClientCommandCompletionContext context);
}
