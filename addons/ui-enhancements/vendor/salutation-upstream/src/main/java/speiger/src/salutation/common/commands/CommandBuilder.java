package speiger.src.salutation.common.commands;

import java.util.Stack;

import speiger.src.salutation.common.commands.ArgumentNode.SuggestionProvider;
import speiger.src.salutation.common.commands.args.IArgument;

public class CommandBuilder {
	Stack<CommandNode> queue = new Stack<>();
	CommandNode current;
	
	public CommandBuilder(String literal) {
		current = new LiteralNode(literal);
	}
	
	public CommandBuilder(String name, IArgument<?> argument) {
		current = new ArgumentNode<>(name, argument);
	}
	
	public CommandBuilder literal(String literal) {
		return push(new LiteralNode(literal));
	}
	
	public CommandBuilder literal(String literal, SalutationCommand command) {
		return push(new LiteralNode(literal).withCommand(command));
	}
	
	public CommandBuilder arg(String name, IArgument<?> type) {
		return push(new ArgumentNode<>(name, type));
	}
	
	public CommandBuilder arg(String name, IArgument<?> type, SalutationCommand command) {
		return push(new ArgumentNode<>(name, type).withCommand(command));
	}
	
	public CommandBuilder arg(String name, IArgument<?> type, SuggestionProvider suggestion) {
		return push(new ArgumentNode<>(name, type).withSuggestion(suggestion));
	}
	
	public CommandBuilder arg(String name, IArgument<?> type, SuggestionProvider suggestion, SalutationCommand command) {
		return push(new ArgumentNode<>(name, type).withSuggestion(suggestion).withCommand(command));
	}
	
	private CommandBuilder push(CommandNode command) {
		current.addChild(command);
		queue.push(current);
		current = command;
		return this;
	}
	
	public CommandBuilder popTop() {
		while(!queue.isEmpty()) {
			current = queue.pop();
		}
		return this;
	}
	
	public CommandBuilder pop() {
		current = queue.pop();
		return this;
	}
	
	public CommandBuilder pop(int layers) {
		while(layers > 0) {
			current = queue.pop();
			layers--;
		}
		return this;
	}
	
	public CommandNode build() {
		while(!queue.isEmpty()) {
			current = queue.pop();
		}
		return current;
	}
}
