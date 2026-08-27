package neofontrender.addons.mixin;

import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import neofontrender.addons.api.command.CommandCompletionPosition;
import neofontrender.addons.api.command.server.ServerCommandCompletionApi;
import neofontrender.addons.api.command.server.ServerCommandCompletionContext;
import neofontrender.addons.command.BuiltinCommandCompletionProviders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Version-specific bridge between vanilla's command manager and the standard provider API. */
@Mixin(CommandHandler.class)
public abstract class MixinCommandHandlerCompletionProviders {
    @Shadow @Final private Map<String, ICommand> commandMap;
    @Shadow protected abstract MinecraftServer getServer();

    @Inject(method = "getTabCompletions", at = @At("RETURN"), cancellable = true)
    private void nfrUi$resolveCompletionProviders(ICommandSender sender, String input,
                                                   @Nullable BlockPos targetPos,
                                                   CallbackInfoReturnable<List<String>> cir) {
        String[] tokens = input.split(" ", -1);
        if (tokens.length <= 1) return;

        String root = tokens[0];
        ICommand command = commandMap.get(root);
        MinecraftServer server = getServer();
        if (command == null || !command.checkPermission(server, sender)) return;

        BuiltinCommandCompletionProviders.ensureRegistered();
        List<String> original = cir.getReturnValue();
        if (original == null) original = Collections.emptyList();
        BlockPos pos = targetPos;
        CommandCompletionPosition completionPos = pos == null ? null
                : new CommandCompletionPosition(pos.getX(), pos.getY(), pos.getZ());
        ServerCommandCompletionContext context = new ServerCommandCompletionContext(
                server, sender, command, root,
                Arrays.copyOfRange(tokens, 1, tokens.length), completionPos, original);
        List<String> resolved = ServerCommandCompletionApi.resolve(context);
        if (resolved != original) cir.setReturnValue(resolved);
    }
}
