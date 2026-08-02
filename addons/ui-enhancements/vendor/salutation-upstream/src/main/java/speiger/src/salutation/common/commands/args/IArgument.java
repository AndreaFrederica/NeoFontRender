package speiger.src.salutation.common.commands.args;

import java.util.List;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public interface IArgument<T> {
	public T parse(StringWalker args, CommandContext context) throws CommandException;
	public List<String> getExamples(CommandContext context, int argumentIndex);
	public default int getArgumentElements() { return 1; }
}
