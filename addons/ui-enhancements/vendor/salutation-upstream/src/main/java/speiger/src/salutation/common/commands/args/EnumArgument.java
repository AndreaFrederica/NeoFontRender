package speiger.src.salutation.common.commands.args;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public class EnumArgument<T extends Enum<T>> implements IArgument<T> {
	Class<T> clz;

	private EnumArgument(Class<T> clz) {
		this.clz = clz;
	}

	public static <T extends Enum<T>> EnumArgument<T> value(Class<T> clz) {
		return new EnumArgument<>(clz);
	}

	@Override
	public T parse(StringWalker args, CommandContext context) throws CommandException {
		return args.readEnum(clz);
	}

	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) {
		List<String> values = new ArrayList<>();
		for(T value : clz.getEnumConstants()) {
			values.add(value.name());
		}
		return values;
	}

}
