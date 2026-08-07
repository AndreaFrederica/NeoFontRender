package speiger.src.salutation.common.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.common.Loader;
import speiger.src.salutation.common.commands.args.StringWalker;
import speiger.src.salutation.common.utils.ChunkPos;
import speiger.src.salutation.common.utils.TranslateUtils;

public abstract class BaseSalutationCommand extends CommandBase {
	String name;
	CommandNode root;

	public BaseSalutationCommand(String baseName) {
		this.name = baseName;
		root = new LiteralNode(baseName);
	}

	protected void addChild(CommandNode child) {
		root.addChild(child);
	}

	protected void addChildren(CommandNode node) {
		node.getChildren().forEach(this::addChild);
	}

	@Override
	public String getCommandName() {
		return name;
	}

	@Override
	public abstract String getCommandUsage(ICommandSender sender);

	@Override
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		CommandContext context = new CommandContext(sender);
		CommandNode node = root.findCurrentNode(new StringWalker(args), context);
		if(context.getException() != null) {
			context.sendFailure(context.getException());
			return;
		}
		if(node == null) {
			context.sendFailure(TranslateUtils.translate("commands.salutation.error.command.notfound", "/"+name+" "+String.join(" ", args)));
			return;
		}
		if(node.getCommand() == null) {
			context.sendFailure(TranslateUtils.translate("commands.salutation.error.command.missingargs"));
			return;
		}
		node.getCommand().execute(context);
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
		CommandContext context = new CommandContext(sender);
		StringWalker walker = new StringWalker(args);
		walker.capTop();
		CommandNode node = root.findCurrentNode(walker, context);
		walker.resetCap();
		return getBestMatch(walker.canRead() ? walker.peek() : null, (node == null ? root : node).getSuggestions(context.getArgumentIndex(), context));
	}

	public static List<String> getBestMatch(String argument, Collection<String> args) {
		List<String> results = new ArrayList<>();
		for(String arg : args) {
			if(argument == null || arg.regionMatches(true, 0, argument, 0, argument.length())) {
				results.add(arg);
			}
		}
		return results;
	}

	public static class CommandContext {
		ICommandSender sender;
		List<CommandNode> nodes = new ArrayList<>();
		Map<String, Object> args = new HashMap<>();
		String argumentId;
		IChatComponent exception;
		int argumentIndex;

		public CommandContext(ICommandSender sender) {
			this.sender = sender;
		}

		public void setException(String argumentId, IChatComponent exception) {
			this.argumentId = argumentId;
			this.exception = exception;
		}

		public IChatComponent getException() {
			return exception;
		}

		public void setArgumentIndex(int index) {
			this.argumentIndex = index;
		}

		public int getArgumentIndex() {
			return argumentIndex;
		}

		public void addNode(CommandNode node) {
			nodes.add(node);
			exception = null;
		}

		public boolean addArgument(String name, Object arg) {
			args.put(name, arg);
			return true;
		}

		public <T> T getArgument(String name, Class<T> clz) {
			Object arg = args.get(name);
			if(arg == null) throw new IllegalStateException("Argument ["+name+"] is missing");
			if(clz.isInstance(arg)) {
				return clz.cast(arg);
			}
			throw new IllegalStateException("Argument ["+name+"] is of type ["+arg.getClass().getSimpleName()+"], but was expected to be ["+clz+"]");
		}

		public <T> T getArgumentOrDefault(String name, Class<T> clz, T defaultValue) {
			Object arg = args.get(name);
			return arg != null && clz.isInstance(arg) ? clz.cast(arg) : defaultValue;
		}

		public <T> boolean hasArgument(String name, Class<T> clz) {
			Object arg = args.get(name);
			return arg != null && clz.isInstance(arg);
		}

		public int getDimension(String name) {
			return getArgumentOrDefault(name, Integer.class, getSenderDimensionId());
		}

		public ICommandSender getSender() {
			return sender;
		}

		public IChatComponent getSenderName() {
			return sender.func_145748_c_();
		}

		public UUID getSenderId() {
			return sender instanceof Entity ? ((Entity)sender).getUniqueID() : null;
		}

		public World getSenderDimension() {
			return sender.getEntityWorld();
		}

		public int getSenderDimensionId() {
			return sender.getEntityWorld().provider.dimensionId;
		}

		public WorldServer getWorld(int dimension) {
			return MinecraftServer.getServer().worldServerForDimension(dimension);
		}

		public ChunkPos getSenderPosition() {
			ChunkCoordinates pos = sender.getPlayerCoordinates();
			return new ChunkPos(pos.posX, pos.posZ);
		}

		public ChunkPos getSpawnPosition() {
			ChunkCoordinates pos = sender.getEntityWorld().getSpawnPoint();
			return new ChunkPos(pos.posX, pos.posZ);
		}

		public void printException() {
			sendFailure(TranslateUtils.translate("commands.salutation.error.command.argument_failed", argumentId, exception));
		}

		/**
		 * This will allow us to detect if the mod is client sided present and that means we can use translations.
		 * If the mod isn't present we will simply server translate the messages and it will allow us to have a improved feature list.
		 * @return if it is installed on the client
		 */
		private boolean isInstalledOnClient() {
			if(!(sender instanceof EntityPlayerMP)) return true;
			// UIE embeds Salutation's client translations directly. Keep the original
			// server fallback when neither the embedded addon nor the old CarbonConfig
			// transport is present.
			return Loader.isModLoaded("neofontrender_ui_enhancements")
					|| Loader.isModLoaded("carbonconfig");
		}

		private void addChatMessage(IChatComponent text) {
			sender.addChatMessage(isInstalledOnClient() ? text : TranslateUtils.serverTranslate(text));
		}

		public void sendSuccess(IChatComponent text) {
			addChatMessage(text);
		}

		public void sendFailure(IChatComponent text) {
			addChatMessage(TranslateUtils.applyTextStyle(text.createCopy(), EnumChatFormatting.RED));
		}
	}
}
