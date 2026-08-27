package neofontrender.addons.api.command.client;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import neofontrender.addons.api.command.CommandCompletionPosition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable local completion request; it never represents server authority or permission. */
public final class ClientCommandCompletionContext {
    private final Minecraft minecraft;
    private final String input;
    private final String root;
    private final String[] arguments;
    private final CommandCompletionPosition targetPosition;
    private final List<String> currentCandidates;

    public ClientCommandCompletionContext(Minecraft minecraft, String input, String root,
                                          String[] arguments,
                                          @Nullable CommandCompletionPosition targetPosition,
                                          Collection<String> currentCandidates) {
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
        this.input = Objects.requireNonNull(input, "input");
        this.root = Objects.requireNonNull(root, "root");
        this.arguments = Objects.requireNonNull(arguments, "arguments").clone();
        this.targetPosition = targetPosition;
        this.currentCandidates = immutableCandidates(currentCandidates);
    }

    public Minecraft minecraft() { return minecraft; }
    public String input() { return input; }
    public String root() { return root; }
    public String[] arguments() { return arguments.clone(); }
    @Nullable public CommandCompletionPosition targetPosition() { return targetPosition; }
    public List<String> currentCandidates() { return currentCandidates; }

    public boolean rootIs(String... names) {
        for (String name : names) {
            if (root.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public List<String> matchingLastArgument(Collection<String> candidates) {
        return CommandBase.getListOfStringsMatchingLastWord(arguments, candidates);
    }

    ClientCommandCompletionContext withCurrentCandidates(Collection<String> candidates) {
        return new ClientCommandCompletionContext(minecraft, input, root, arguments,
                targetPosition, candidates);
    }

    private static List<String> immutableCandidates(Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(candidates));
    }
}
