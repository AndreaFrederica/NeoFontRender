package speiger.src.salutation.common.commands;

import java.util.Collections;
import java.util.List;

import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;

public class LiteralNode extends CommandNode {
	String name;
	
	public LiteralNode(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() { return name; }
	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) { return Collections.singletonList(name); }
}
