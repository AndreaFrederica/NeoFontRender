package speiger.src.salutation.common.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;
import speiger.src.salutation.common.commands.args.IArgument;
import speiger.src.salutation.common.commands.args.StringWalker;
import speiger.src.salutation.common.utils.TranslateUtils;

public class ArgumentNode<T> extends CommandNode {
	String name;
	IArgument<T> argument;
	SuggestionProvider customSuggestions;

	public ArgumentNode(String name, IArgument<T> argument) {
		this.name = name;
		this.argument = argument;
	}

	public ArgumentNode<T> withSuggestion(SuggestionProvider customSuggestions) {
		this.customSuggestions = customSuggestions;
		return this;
	}

	@Override
	public String getName() { return name; }

	public boolean isValid(StringWalker walker, CommandContext context) {
		try {
			T arg = argument.parse(walker, context);
			if(arg != null) return context.addArgument(name, arg);
		}
		catch(CommandException e) {
			context.setException(name, TranslateUtils.translate(e.getMessage(), e.getErrorOjbects()));
		}
		return false;
	}

	public int getArgumentElements() {
		return argument.getArgumentElements();
	}

	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) {
		if(customSuggestions != null) {
			List<String> result = new ArrayList<>();
			customSuggestions.collectSuggestions(context, argumentIndex, result::add);
			return result;
		}
		return argument.getExamples(context, argumentIndex);
	}

	public static interface SuggestionProvider {
		public void collectSuggestions(CommandContext context, int argumentIndex, Consumer<String> suggestions);
	}
}
