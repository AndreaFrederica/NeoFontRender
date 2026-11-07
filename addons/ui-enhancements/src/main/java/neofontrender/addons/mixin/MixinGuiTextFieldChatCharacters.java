package neofontrender.addons.mixin;

import neofontrender.addons.chat.ClientChatPolicy;
import neofontrender.addons.chat.network.ChatCharacterPolicy;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatAllowedCharacters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep vanilla selection, validator, responder and maximum-length handling intact. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldChatCharacters {
    // Other input mods (for example InGameIME) can rewrite these methods before
    // this mixin runs. Missing call sites disable this optional compatibility hook
    // instead of aborting the entire client during class transformation.
    @Redirect(method = "textboxKeyTyped", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ChatAllowedCharacters;isAllowedCharacter(C)Z"), require = 0)
    private boolean nfrUi$allowedCharacter(char value) {
        if (value != '\u00A7') return ChatAllowedCharacters.isAllowedCharacter(value);
        return ChatCharacterPolicy.isAllowed(value,
                ClientChatPolicy.allowsSectionSignInput((GuiTextField) (Object) this));
    }

    @Redirect(method = "writeText", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ChatAllowedCharacters;filterAllowedCharacters(Ljava/lang/String;)Ljava/lang/String;"), require = 0)
    private String nfrUi$filterChatText(String value) {
        if (!ChatCharacterPolicy.containsSectionSign(value)
                || !ClientChatPolicy.allowsSectionSignInput((GuiTextField) (Object) this)) {
            return ChatAllowedCharacters.filterAllowedCharacters(value);
        }
        return ChatCharacterPolicy.filter(value, true);
    }
}
