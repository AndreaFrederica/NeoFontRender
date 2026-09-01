package neofontrender.addons.mixin.compat;

import neofontrender.addons.chat.CleanroomCommandCompletionCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Cleanroom's chat-only suggestion presentation inert while UIE owns completion. */
@Pseudo
@Mixin(targets = "com.cleanroommc.client.chat.suggestion.SuggestionList", remap = false)
public abstract class MixinCleanroomSuggestionList {
    @Shadow
    @Final
    private boolean commandBlockMode;

    @Inject(method = "isVisible()Z", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$hideFromInputRouting(CallbackInfoReturnable<Boolean> cir) {
        if (CleanroomCommandCompletionCompat.suppressCleanroomSuggestions(commandBlockMode)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInvisible()Z", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$hideFromRendering(CallbackInfoReturnable<Boolean> cir) {
        if (CleanroomCommandCompletionCompat.suppressCleanroomSuggestions(commandBlockMode)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawCommandColor", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$suppressCommandColor(CallbackInfo ci) {
        if (CleanroomCommandCompletionCompat.suppressCleanroomSuggestions(commandBlockMode)) {
            ci.cancel();
        }
    }
}
