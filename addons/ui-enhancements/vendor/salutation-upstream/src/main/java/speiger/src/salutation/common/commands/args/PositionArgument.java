package speiger.src.salutation.common.commands.args;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandException;
import speiger.src.salutation.common.commands.BaseSalutationCommand.CommandContext;
import speiger.src.salutation.common.utils.ChunkPos;

public class PositionArgument implements IArgument<ChunkPos> {
	private static final List<String> FIRST = Arrays.asList(new String[]{"0", "~", "^", "^1", "~1", "b2"});
	private static final List<String> SECOND = Arrays.asList(new String[]{"0", "~", "^", "^-5", "~-5", "b-5"});
	boolean chunkPos;

	protected PositionArgument(boolean chunkPos) { this.chunkPos = chunkPos; }

	public static PositionArgument blockPos() {
		return new PositionArgument(false);
	}

	public static PositionArgument chunkPos() {
		return new PositionArgument(true);
	}

	@Override
	public ChunkPos parse(StringWalker args, CommandContext context) throws CommandException {
		ChunkPos player = context.getSenderPosition();
		ChunkPos spawn = context.getSpawnPosition();
		if(chunkPos) {
			player = player.toChunkPos();
			spawn = spawn.toChunkPos();
		}
		return new ChunkPos(args.parsePosition(player.x, spawn.x, chunkPos), args.parsePosition(player.z, spawn.z, chunkPos));
	}

	@Override
	public int getArgumentElements() { return 2; }
	@Override
	public List<String> getExamples(CommandContext context, int argumentIndex) { return argumentIndex == 0 ? FIRST : SECOND; }
}
