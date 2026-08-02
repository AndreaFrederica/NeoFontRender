package speiger.src.salutation.common.commands.args;

import net.minecraft.command.CommandException;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.SyntaxErrorException;

public class StringWalker {
	String[] arguments;
	int max;
	int index;
	
	public StringWalker(String[] arguments) {
		this.arguments = arguments;
		this.max = arguments.length;
	}
	
	public StringWalker(String[] arguments, int index) {
		this.arguments = arguments;
		this.max = arguments.length;
		this.index = index;
	}
	
	public void apply(StringWalker other) {
		index = other.index;
	}
	
	public StringWalker copy() {
		return new StringWalker(arguments, index);
	}
	
	public void capTop() {
		max--;
	}
	
	public void resetCap() {
		max = arguments.length;
	}
	
	public boolean canRead() {
		return index < max;
	}
	
	public String peek() {
		return arguments[index];
	}
	
	public String poll() {
		return arguments[index++];
	}
	
	public void advance(int amount) {
		index += amount;
	}
	
	public void skip() {
		index++;
	}
	
	public void undo() {
		index--;
	}
	
	public int getIndex() {
		return index;
	}
	
	public int parsePosition(int playerPos, int worldPos, boolean chunk) throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		String value = peek();
		try {
			int result = 0;
			int offset = 0;
			if(value.charAt(0) == '^') {
				result += worldPos;
				offset+=1;
			}
			else if(value.charAt(0) == '~') {
				result += playerPos;
				offset+=1;
			}
			boolean isBlock = value.charAt(value.length()-1) == 'b';
			String subString = value.substring(offset, value.length() - (isBlock ? 1 : 0));
			if(!subString.isEmpty()) {
				result += Integer.parseInt(subString) / (isBlock && chunk ? 16 : 1);
			}
			skip();
			return result;
		}
		catch(Exception e) { throw new NumberInvalidException("commands.salutation.error.parse.int", value, e); }
	}
	
	public int parseChunkOrBlock(boolean chunk) throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		String value = peek();
		try {
			boolean isBlock = value.charAt(value.length()-1) == 'b';
			int result = Integer.parseInt(value.substring(0, value.length() - (isBlock ? 1 : 0))) / (isBlock && chunk ? 16 : 1);
			skip();
			return result;
		}
		catch(Exception e) { throw new NumberInvalidException("commands.salutation.error.parse.int", value, e); }
	}
	
	public <T extends Enum<T>> T readEnum(Class<T> clz) throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		try { return Enum.valueOf(clz, poll()); }
		catch(Exception e) { throw new NumberInvalidException("commands.salutation.error.parse.int", arguments[index-1], e); }
	}
	
	public boolean readBoolean() throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		String arg = poll();
		if(arg.equalsIgnoreCase("true")) return true;
		if(arg.equalsIgnoreCase("false")) return false;
		throw new SyntaxErrorException("commands.salutation.error.parse.boolean", arguments[index-1]);
	}
	
	
	public int readInt() throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		try { return Integer.parseInt(poll()); }
		catch(Exception e) { throw new NumberInvalidException("commands.salutation.error.parse.int", arguments[index-1], e); }
	}
	
	public long readLong() throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		try { return Long.parseLong(poll()); }
		catch(Exception e) { throw new NumberInvalidException("commands.salutation.error.parse.long", arguments[index-1], e); }
	}
	
	public double readDouble() throws CommandException {
		if(!canRead()) throw new CommandException("commands.salutation.error.parse.missing.args");
		try { return Double.parseDouble(poll()); }
		catch(Exception e) { throw new NumberInvalidException("commands.salutation.error.parse.double", arguments[index-1], e); }
	}
}
