package neofontrender.addons.api.command.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.command.CommandCompletionPosition;
import neofontrender.addons.api.command.CommandCompletionRegistration;
import neofontrender.addons.command.CommandCompletionPipeline;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Public registry for local, non-authoritative client command completion providers. */
public final class ClientCommandCompletionApi {
    public static final int API_VERSION = 1;

    private static final CommandCompletionPipeline<ClientCommandCompletionContext> PIPELINE =
            new CommandCompletionPipeline<>("RevoUI/ClientCommandCompletion");
    private static final CommandCompletionPipeline.ContextAdapter<ClientCommandCompletionContext>
            CONTEXT_ADAPTER = new CommandCompletionPipeline.ContextAdapter<ClientCommandCompletionContext>() {
                @Override
                public Collection<String> currentCandidates(ClientCommandCompletionContext context) {
                    return context.currentCandidates();
                }

                @Override
                public ClientCommandCompletionContext withCurrentCandidates(
                        ClientCommandCompletionContext context, Collection<String> candidates) {
                    return context.withCurrentCandidates(candidates);
                }
            };

    private ClientCommandCompletionApi() {}

    public static CommandCompletionRegistration registerProvider(
            ResourceLocation id, int priority, ClientCommandCompletionProvider provider) {
        return PIPELINE.register(id, priority, provider::complete);
    }

    public static String[] resolve(String input,
                                   @Nullable CommandCompletionPosition targetPosition,
                                   String[] originalCandidates) {
        String normalized = input.startsWith("/") ? input.substring(1) : input;
        String[] tokens = normalized.split(" ", -1);
        String root = tokens.length == 0 ? "" : tokens[0];
        String[] arguments = tokens.length <= 1
                ? new String[0] : Arrays.copyOfRange(tokens, 1, tokens.length);
        Collection<String> original = originalCandidates == null
                ? Collections.emptyList() : Arrays.asList(originalCandidates);
        ClientCommandCompletionContext context = new ClientCommandCompletionContext(
                Minecraft.getMinecraft(), input, root, arguments, targetPosition, original);
        List<String> resolved = PIPELINE.resolve(context, CONTEXT_ADAPTER);
        return resolved.toArray(new String[0]);
    }
}
