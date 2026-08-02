package speiger.src.salutation.common.commands.args;

import java.util.Arrays;
import java.util.List;

import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public class StringArgument implements IArgument<String> {
	
	public static StringArgument text() {
		return new StringArgument();
	}
	
	@Override
	public String parse(StringWalker args, CommandContext context) { return args.poll(); }
	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) { return Arrays.asList(new String[] {"word", "words_with_underline"}); }
	
}
