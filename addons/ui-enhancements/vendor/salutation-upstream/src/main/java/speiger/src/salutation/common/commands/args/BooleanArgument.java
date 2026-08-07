package speiger.src.salutation.common.commands.args;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public class BooleanArgument implements IArgument<Boolean> {
	private static final List<String> EXAMPLES = Arrays.asList(new String[] {"true", "false"});

	public static BooleanArgument bool() {
		return new BooleanArgument();
	}

	@Override
	public Boolean parse(StringWalker args, CommandContext context) throws CommandException {
		return args.readBoolean();
	}

	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) { return EXAMPLES; }

}
