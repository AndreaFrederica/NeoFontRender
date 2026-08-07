package speiger.src.salutation.common.commands.args;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public class LongArgument implements IArgument<Long> {
	public static final DecimalFormat NUMBERS = new DecimalFormat("###,###", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private static final List<String> EXAMPLES = Arrays.asList(new String[] {"0", "123", "-123"});

	long min;
	long max;

	protected LongArgument(long min, long max) {
		this.min = min;
		this.max = max;
	}

	public static LongArgument value() {
		return new LongArgument(Long.MIN_VALUE, Long.MAX_VALUE);
	}

	public static LongArgument max(long max) {
		return new LongArgument(Long.MIN_VALUE, max);
	}

	public static LongArgument min(long min) {
		return new LongArgument(min, Long.MAX_VALUE);
	}

	public static LongArgument range(long min, long max) {
		return new LongArgument(min, max);
	}

	@Override
	public Long parse(StringWalker args, CommandContext context) throws CommandException {
		long value = args.readLong();
		if(value < min) throw new CommandException("commands.salutation.error.parse.to_small", NUMBERS.format(value), NUMBERS.format(min));
		if(value > max) throw new CommandException("commands.salutation.error.parse.to_big", NUMBERS.format(value), NUMBERS.format(max));
		return value;
	}

	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) {
		return EXAMPLES;
	}

}
