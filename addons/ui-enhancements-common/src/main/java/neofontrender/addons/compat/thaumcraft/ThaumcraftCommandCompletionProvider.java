package neofontrender.addons.compat.thaumcraft;

import net.minecraftforge.fml.common.Loader;
import neofontrender.addons.api.command.CommandCompletionResult;
import neofontrender.addons.api.command.server.ServerCommandCompletionContext;
import neofontrender.addons.api.command.server.ServerCommandCompletionProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Standard completion adapter for TC6's hand-written command parser. */
public final class ThaumcraftCommandCompletionProvider implements ServerCommandCompletionProvider {
    private static final String COMMAND_CLASS = "thaumcraft.common.lib.CommandThaumcraft";
    private static final List<String> ACTIONS =
            Arrays.asList("help", "reload", "research", "warp");
    private static final List<String> RESEARCH_ACTIONS =
            Arrays.asList("list", "all", "reset", "revoke");
    private static final List<String> WARP_ACTIONS = Arrays.asList("add", "set");
    private static final List<String> WARP_TYPES = Arrays.asList("PERM", "TEMP");

    @Override
    public CommandCompletionResult complete(ServerCommandCompletionContext context) {
        if (!Loader.isModLoaded("thaumcraft") || !context.commandClassIs(COMMAND_CLASS)) {
            return CommandCompletionResult.pass();
        }
        List<String> players = Arrays.asList(context.server().getOnlinePlayerNames());
        List<String> candidates = complete(context.arguments(), players, researchKeys());
        return CommandCompletionResult.fallback(candidates);
    }

    static List<String> complete(String[] arguments, Collection<String> players,
                                 Collection<String> researchKeys) {
        if (arguments == null || arguments.length == 0) return Collections.emptyList();
        if (arguments.length == 1) return matching(arguments, ACTIONS);

        if ("research".equalsIgnoreCase(arguments[0])) {
            return completeResearch(arguments, players, researchKeys);
        }
        if ("warp".equalsIgnoreCase(arguments[0])) {
            return completeWarp(arguments, players);
        }
        return Collections.emptyList();
    }

    private static List<String> completeResearch(String[] arguments,
                                                  Collection<String> players,
                                                  Collection<String> researchKeys) {
        if (arguments.length == 2) {
            List<String> candidates = new ArrayList<>();
            candidates.add("list");
            candidates.addAll(players);
            return matching(arguments, candidates);
        }
        if ("list".equalsIgnoreCase(arguments[1])) return Collections.emptyList();
        if (arguments.length == 3) {
            List<String> candidates = new ArrayList<>(RESEARCH_ACTIONS);
            candidates.addAll(researchKeys);
            return matching(arguments, candidates);
        }
        if (arguments.length == 4 && "revoke".equalsIgnoreCase(arguments[2])) {
            return matching(arguments, researchKeys);
        }
        return Collections.emptyList();
    }

    private static List<String> completeWarp(String[] arguments,
                                              Collection<String> players) {
        if (arguments.length == 2) return matching(arguments, players);
        if (arguments.length == 3) return matching(arguments, WARP_ACTIONS);
        if (arguments.length == 5) return matching(arguments, WARP_TYPES);
        return Collections.emptyList();
    }

    private static List<String> matching(String[] arguments, Collection<String> candidates) {
        return net.minecraft.command.CommandBase.getListOfStringsMatchingLastWord(
                arguments, candidates);
    }

    /** Samples TC's live maps so a runtime research reload immediately changes candidates. */
    private static Collection<String> researchKeys() {
        try {
            ClassLoader loader = ThaumcraftCommandCompletionProvider.class.getClassLoader();
            Class<?> categoriesClass = Class.forName(
                    "thaumcraft.api.research.ResearchCategories", false, loader);
            Field categoriesField = categoriesClass.getField("researchCategories");
            Object categoriesValue = categoriesField.get(null);
            if (!(categoriesValue instanceof Map)) return Collections.emptyList();

            Set<String> keys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (Object category : ((Map<?, ?>) categoriesValue).values()) {
                if (category == null) continue;
                Field researchField = category.getClass().getField("research");
                Object researchValue = researchField.get(category);
                if (!(researchValue instanceof Map)) continue;
                for (Object key : ((Map<?, ?>) researchValue).keySet()) {
                    if (key instanceof String) keys.add((String) key);
                }
            }
            return keys;
        } catch (ReflectiveOperationException | LinkageError | SecurityException ignored) {
            return Collections.emptyList();
        }
    }
}
