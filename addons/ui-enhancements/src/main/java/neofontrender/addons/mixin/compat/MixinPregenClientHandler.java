package neofontrender.addons.mixin.compat;

import neofontrender.addons.chat.PregenChatCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Only overrides the advanced-chat option, preserving the handler's other warning dialogs. */
@Pseudo
@Mixin(targets = "pregenerator.impl.client.ClientHandler", remap = false)
public abstract class MixinPregenClientHandler {
    @Redirect(method = "onGuiOpen", at = @At(value = "INVOKE",
            target = "Lcarbonconfiglib/config/ConfigEntry$BoolValue;get()Z"), require = 1, remap = false)
    private boolean nfrUi$keepChatOwner(@Coerce Object entry) {
        return PregenChatCompat.optionValue(entry);
    }
}
