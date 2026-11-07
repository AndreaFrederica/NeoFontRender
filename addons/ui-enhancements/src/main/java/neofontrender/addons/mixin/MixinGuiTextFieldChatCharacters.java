package neofontrender.addons.mixin;

import neofontrender.addons.chat.ClientChatPolicy;
import neofontrender.addons.chat.network.ChatCharacterPolicy;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ChatAllowedCharacters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla selection, validator, responder and maximum-length handling intact. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldChatCharacters {
    @Shadow private String text;
    @Shadow private int cursorPosition;
    @Shadow private int selectionEnd;

    @Unique private static final char NFR_UI_SECTION_SIGN_SENTINEL = '\uE000';
    @Unique private boolean nfrUi$sectionSignSentinelActive;
    @Unique private int nfrUi$sectionSignInsertionStart;

    /**
     * InGameIME also redirects ChatAllowedCharacters.filterAllowedCharacters and then applies
     * its cluster-safe truncation. Preserve that path by hiding an allowed section sign behind
     * a printable sentinel until the vanilla/InGameIME write has completed.
     */
    @ModifyVariable(method = "writeText", at = @At("HEAD"), argsOnly = true, require = 0)
    private String nfrUi$prepareSectionSignForInputMod(String value) {
        nfrUi$sectionSignSentinelActive = false;
        if (!ChatCharacterPolicy.containsSectionSign(value)
                || !ClientChatPolicy.allowsSectionSignInput((GuiTextField) (Object) this)) {
            return value;
        }
        nfrUi$sectionSignSentinelActive = true;
        nfrUi$sectionSignInsertionStart = Math.min(cursorPosition, selectionEnd);
        return value.replace('\u00A7', NFR_UI_SECTION_SIGN_SENTINEL);
    }

    @Inject(method = "writeText", at = @At("TAIL"), require = 0)
    private void nfrUi$restoreSectionSignAfterInputMod(CallbackInfo ci) {
        if (!nfrUi$sectionSignSentinelActive) return;
        nfrUi$sectionSignSentinelActive = false;

        int start = Math.max(0, Math.min(nfrUi$sectionSignInsertionStart, text.length()));
        int end = Math.max(start, Math.min(cursorPosition, text.length()));
        if (start == end) return;

        String inserted = text.substring(start, end);
        if (inserted.indexOf(NFR_UI_SECTION_SIGN_SENTINEL) < 0) return;
        text = text.substring(0, start)
                + inserted.replace(NFR_UI_SECTION_SIGN_SENTINEL, '\u00A7')
                + text.substring(end);
    }

    // Other input mods (for example InGameIME) can rewrite these methods before
    // this mixin runs. These redirects remain a fallback for installations without
    // InGameIME; the sentinel path above keeps both redirect chains compatible.
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
