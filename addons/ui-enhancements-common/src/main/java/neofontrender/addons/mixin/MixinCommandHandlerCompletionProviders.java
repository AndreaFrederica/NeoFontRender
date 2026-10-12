package neofontrender.addons.mixin;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import neofontrender.addons.api.command.server.ServerCommandCompletionApi;
import neofontrender.addons.api.command.server.ServerCommandCompletionContext;
import neofontrender.addons.command.BuiltinCommandCompletionProviders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Version-specific bridge between vanilla's command manager and the standard provider API.
 *
 * <p>1.7.10 resolves tab completions through {@code getPossibleCommands(sender, input)} and carries
 * no target block position, so providers see a null completion position.</p>
 */
@Mixin(CommandHandler.class)
public abstract class MixinCommandHandlerCompletionProviders {
    @Shadow @Final private Map<String, ICommand> commandMap;

    @Inject(method = "getPossibleCommands(Lnet/minecraft/command/ICommandSender;Ljava/lang/String;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private void nfrUi$resolveCompletionProviders(ICommandSender sender, String input,
                                                   CallbackInfoReturnable<List<String>> cir) {
        String[] tokens = input.split(" ", -1);
        if (tokens.length <= 1) return;

        String root = tokens[0];
        ICommand command = commandMap.get(root);
        MinecraftServer server = MinecraftServer.getServer();
        if (command == null || server == null || !command.canCommandSenderUseCommand(sender)) return;

        BuiltinCommandCompletionProviders.ensureRegistered();
        List<String> original = cir.getReturnValue();
        if (original == null) original = Collections.emptyList();
        ServerCommandCompletionContext context = new ServerCommandCompletionContext(
                server, sender, command, root,
                Arrays.copyOfRange(tokens, 1, tokens.length), null, original);
        List<String> resolved = ServerCommandCompletionApi.resolve(context);
        if (resolved != original) cir.setReturnValue(resolved);
    }
}
