package speiger.src.salutation.common.commands.args;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public class DoubleArgument implements IArgument<Double> {
	public static final DecimalFormat FLOATING_NUMBERS = new DecimalFormat("###,###.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private static final List<String> EXAMPLES = Arrays.asList(new String[] {"0", "1.2", ".5", "-1", "-.5", "-1234.56"});
	
	double min;
	double max;
	
	protected DoubleArgument(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	public static DoubleArgument value() {
		return new DoubleArgument(-Double.MAX_VALUE, Double.MAX_VALUE);
	}
	
	public static DoubleArgument max(double max) {
		return new DoubleArgument(-Double.MAX_VALUE, max);
	}
	
	public static DoubleArgument min(double min) {
		return new DoubleArgument(min, Double.MAX_VALUE);
	}
	
	public static DoubleArgument range(double min, double max) {
		return new DoubleArgument(min, max);
	}
	
	@Override
	public Double parse(StringWalker args, CommandContext context) throws CommandException {
		double value = args.readDouble();
		if(value < min) throw new CommandException("commands.salutation.error.parse.to_small", FLOATING_NUMBERS.format(value), FLOATING_NUMBERS.format(min));
		if(value > max) throw new CommandException("commands.salutation.error.parse.to_big", FLOATING_NUMBERS.format(value), FLOATING_NUMBERS.format(max));	
		return value;
	}
	
	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) {
		return EXAMPLES;
	}
}
