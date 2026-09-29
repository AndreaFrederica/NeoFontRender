package neofontrender.addons.mixin;

import neofontrender.addons.chat.network.ChatCharacterPolicy;
import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies the shared policy only to the server's chat packet validation. */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinServerChatCharacters {
    @Redirect(method = "processChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatAllowedCharacters;isAllowedCharacter(C)Z"), require = 1)
    private boolean nfr$serverCharacter(char value) {
        return ChatCharacterPolicy.isAllowed(value, ChatCharacterPolicy.serverAllowsSectionSign());
    }
}
