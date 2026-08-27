package neofontrender.addons.api.command.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import neofontrender.addons.api.command.CommandCompletionPosition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable authoritative completion request from the server command manager. */
public final class ServerCommandCompletionContext {
    private final MinecraftServer server;
    private final ICommandSender sender;
    private final ICommand command;
    private final String root;
    private final String[] arguments;
    private final CommandCompletionPosition targetPosition;
    private final List<String> currentCandidates;

    public ServerCommandCompletionContext(MinecraftServer server, ICommandSender sender,
                                          ICommand command, String root, String[] arguments,
                                          @Nullable CommandCompletionPosition targetPosition,
                                          Collection<String> currentCandidates) {
        this.server = Objects.requireNonNull(server, "server");
        this.sender = Objects.requireNonNull(sender, "sender");
        this.command = Objects.requireNonNull(command, "command");
        this.root = Objects.requireNonNull(root, "root");
        this.arguments = Objects.requireNonNull(arguments, "arguments").clone();
        this.targetPosition = targetPosition;
        this.currentCandidates = immutableCandidates(currentCandidates);
    }

    public MinecraftServer server() { return server; }
    public ICommandSender sender() { return sender; }
    public ICommand command() { return command; }
    public String root() { return root; }
    public String[] arguments() { return arguments.clone(); }
    @Nullable public CommandCompletionPosition targetPosition() { return targetPosition; }
    public List<String> currentCandidates() { return currentCandidates; }

    public boolean commandClassIs(String className) {
        return command.getClass().getName().equals(className);
    }

    public boolean rootIs(String... names) {
        for (String name : names) {
            if (root.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public List<String> matchingLastArgument(Collection<String> candidates) {
        return CommandBase.getListOfStringsMatchingLastWord(arguments, candidates);
    }

    ServerCommandCompletionContext withCurrentCandidates(Collection<String> candidates) {
        return new ServerCommandCompletionContext(server, sender, command, root, arguments,
                targetPosition, candidates);
    }

    private static List<String> immutableCandidates(Collection<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(candidates));
    }
}
