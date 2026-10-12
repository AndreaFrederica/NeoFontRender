package neofontrender.addons.api.command.server;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.command.CommandCompletionRegistration;
import neofontrender.addons.command.CommandCompletionPipeline;

import java.util.Collection;
import java.util.List;

/** Public registry for authoritative dedicated and integrated server completion providers. */
public final class ServerCommandCompletionApi {
    public static final int API_VERSION = 1;

    private static final CommandCompletionPipeline<ServerCommandCompletionContext> PIPELINE =
            new CommandCompletionPipeline<>("RevoUI/ServerCommandCompletion");
    private static final CommandCompletionPipeline.ContextAdapter<ServerCommandCompletionContext>
            CONTEXT_ADAPTER = new CommandCompletionPipeline.ContextAdapter<ServerCommandCompletionContext>() {
                @Override
                public Collection<String> currentCandidates(ServerCommandCompletionContext context) {
                    return context.currentCandidates();
                }

                @Override
                public ServerCommandCompletionContext withCurrentCandidates(
                        ServerCommandCompletionContext context, Collection<String> candidates) {
                    return context.withCurrentCandidates(candidates);
                }
            };

    private ServerCommandCompletionApi() {}

    public static CommandCompletionRegistration registerProvider(
            ResourceLocation id, int priority, ServerCommandCompletionProvider provider) {
        return PIPELINE.register(id, priority, provider::complete);
    }

    public static List<String> resolve(ServerCommandCompletionContext context) {
        return PIPELINE.resolve(context, CONTEXT_ADAPTER);
    }
}
