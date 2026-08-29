package neofontrender.addons.command;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.command.CommandCompletionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandCompletionPipelineTest {
    private static final CommandCompletionPipeline.ContextAdapter<Context> ADAPTER =
            new CommandCompletionPipeline.ContextAdapter<Context>() {
                @Override
                public Collection<String> currentCandidates(Context context) {
                    return context.candidates;
                }

                @Override
                public Context withCurrentCandidates(Context context,
                                                     Collection<String> candidates) {
                    return new Context(candidates);
                }
            };

    @Test
    void fallbackOnlyRunsWhenOriginalCommandReturnedNothing() {
        CommandCompletionPipeline<Context> pipeline = pipeline();
        pipeline.register(id("fallback"), 0,
                context -> CommandCompletionResult.fallback(
                        Collections.singletonList("provided")));

        assertEquals(Collections.singletonList("provided"),
                pipeline.resolve(new Context(Collections.emptyList()), ADAPTER));
        assertEquals(Collections.singletonList("vanilla"),
                pipeline.resolve(new Context(Collections.singletonList("vanilla")), ADAPTER));
    }

    @Test
    void priorityAndModesComposeDeterministically() {
        CommandCompletionPipeline<Context> pipeline = pipeline();
        pipeline.register(id("append-last"), 0,
                context -> CommandCompletionResult.append(Arrays.asList("shared", "last")));
        pipeline.register(id("replace-first"), 100,
                context -> CommandCompletionResult.replace(Arrays.asList("first", "shared")));

        assertEquals(Arrays.asList("first", "shared", "last"),
                pipeline.resolve(new Context(Collections.singletonList("vanilla")), ADAPTER));
    }

    @Test
    void newerRegistrationWithTheSameIdReplacesTheOldProvider() {
        CommandCompletionPipeline<Context> pipeline = pipeline();
        pipeline.register(id("adapter"), 0,
                context -> CommandCompletionResult.replace(Collections.singletonList("old")));
        pipeline.register(id("adapter"), 0,
                context -> CommandCompletionResult.replace(Collections.singletonList("new")));

        assertEquals(Collections.singletonList("new"),
                pipeline.resolve(new Context(Collections.emptyList()), ADAPTER));
    }

    private static CommandCompletionPipeline<Context> pipeline() {
        return new CommandCompletionPipeline<>("RevoUI/CommandCompletionTest");
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }

    private static final class Context {
        private final List<String> candidates;

        private Context(Collection<String> candidates) {
            this.candidates = new ArrayList<>(candidates);
        }
    }
}
