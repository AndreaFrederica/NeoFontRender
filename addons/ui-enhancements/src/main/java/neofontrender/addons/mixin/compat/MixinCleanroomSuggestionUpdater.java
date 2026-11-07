package neofontrender.addons.mixin.compat;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import neofontrender.addons.chat.CleanroomCommandCompletionCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents Cleanroom from issuing a second completion request for UIE-owned chat input. */
@Pseudo
@Mixin(targets = "com.cleanroommc.client.chat.suggestion.SuggestionUpdater", remap = false)
public abstract class MixinCleanroomSuggestionUpdater {
    @Shadow
    @Final
    private boolean commandBlockMode;

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/NetHandlerPlayClient;sendPacket(Lnet/minecraft/network/Packet;)V",
            remap = true), require = 1, remap = false)
    private void nfrUi$suppressInitialRequest(NetHandlerPlayClient connection, Packet<?> packet) {
        if (!CleanroomCommandCompletionCompat.suppressCleanroomSuggestions(commandBlockMode)) {
            connection.sendPacket(packet);
        }
    }

    @Inject(method = "refresh()V", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$suppressRefresh(CallbackInfo ci) {
        if (CleanroomCommandCompletionCompat.suppressCleanroomSuggestions(commandBlockMode)) {
            ci.cancel();
        }
    }

    @Inject(method = "onServerCompletions([Ljava/lang/String;)V", at = @At("HEAD"),
            cancellable = true, require = 1, remap = false)
    private void nfrUi$suppressServerCompletions(CallbackInfo ci) {
        if (CleanroomCommandCompletionCompat.suppressCleanroomSuggestions(commandBlockMode)) {
            ci.cancel();
        }
    }
}
