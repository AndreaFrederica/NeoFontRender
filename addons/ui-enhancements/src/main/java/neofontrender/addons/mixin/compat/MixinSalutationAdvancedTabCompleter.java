package neofontrender.addons.mixin.compat;

import neofontrender.addons.chat.ChatSuggestionPopup;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import neofontrender.addons.chat.ExternalChatCompat;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "speiger.src.salutation.client.gui.chat.AdvancedTabCompleter", remap = false)
public abstract class MixinSalutationAdvancedTabCompleter extends TabCompleter {
    @Shadow(remap = false) protected int offset;
    @Shadow(remap = false) public abstract void select(int index);

    private ChatSuggestionPopup.Layout nfrUi$layout;

    protected MixinSalutationAdvancedTabCompleter(GuiTextField textField, boolean hasTargetBlock) {
        super(textField, hasTargetBlock);
    }

    @Inject(method = "requestUpdate", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$disableRequests(CallbackInfo ci) {
        if (EnhancedChatConfigAccess.commandCompletionEnabled()) return;
        this.completions.clear();
        this.nfrUi$layout = null;
        ci.cancel();
    }

    @Inject(method = "onKeyPress", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$disableKeyboard(int key, CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()) cir.setReturnValue(false);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfrUi$renderTabbyChatSuggestions(
            int mouseX, int mouseY, FontRenderer font, CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()) {
            this.nfrUi$layout = null;
            ci.cancel();
            return;
        }
        ExternalChatCompat.InputGeometry geometry =
                ExternalChatCompat.getSalutationInput(this.textField);
        if (geometry == null) return;
        this.nfrUi$layout = ChatSuggestionPopup.draw(this.textField, this.completions,
                this.offset, this.completionIdx, geometry, mouseX, mouseY, font);
        ci.cancel();
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfrUi$selectTabbyChatSuggestion(
            int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()) {
            cir.setReturnValue(false);
            return;
        }
        if (ExternalChatCompat.getSalutationInput(this.textField) == null) return;
        int row = this.nfrUi$layout == null ? -1 : this.nfrUi$layout.rowAt(mouseX, mouseY);
        if (row < 0) {
            cir.setReturnValue(false);
            return;
        }
        this.select(this.offset + row);
        cir.setReturnValue(true);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfrUi$scrollTabbyChatSuggestions(
            int mouseX, int mouseY, int scroll, CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()) {
            cir.setReturnValue(false);
            return;
        }
        if (ExternalChatCompat.getSalutationInput(this.textField) == null) return;
        if (this.nfrUi$layout == null || this.nfrUi$layout.rowAt(mouseX, mouseY) < 0) {
            cir.setReturnValue(false);
            return;
        }
        this.offset = Math.max(0, Math.min(this.offset + scroll,
                Math.max(0, this.completions.size() - ChatSuggestionPopup.MAX_VISIBLE)));
        cir.setReturnValue(true);
    }
}
