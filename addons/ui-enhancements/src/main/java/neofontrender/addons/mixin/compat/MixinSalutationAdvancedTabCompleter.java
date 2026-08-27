package neofontrender.addons.mixin.compat;

import neofontrender.addons.chat.ChatSuggestionPopup;
import neofontrender.addons.chat.CommandCompletionCandidates;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import neofontrender.addons.chat.ExternalChatCompat;
import neofontrender.addons.api.command.CommandCompletionPosition;
import neofontrender.addons.api.command.client.ClientCommandCompletionApi;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ListIterator;

@Pseudo
@Mixin(targets = "speiger.src.salutation.client.gui.chat.AdvancedTabCompleter", remap = false)
public abstract class MixinSalutationAdvancedTabCompleter extends TabCompleter {
    @Shadow(remap = false) protected int offset;
    @Shadow(remap = false) public abstract void select(int index);

    private ChatSuggestionPopup.Layout nfrUi$layout;
    private CommandCompletionCandidates.Merge nfrUi$mergedCompletions;
    private String[] nfrUi$clientCompletions = new String[0];

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

    @Inject(method = "requestUpdate", at = @At("RETURN"), require = 1, remap = false)
    private void nfrUi$captureClientCompletions(CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()
                || this.textField.getText().isEmpty()
                || this.textField.getCursorPosition() == 0) {
            this.nfrUi$clientCompletions = new String[0];
            return;
        }
        String prefix = this.textField.getText().substring(
                0, this.textField.getCursorPosition());
        BlockPos pos = this.getTargetBlockPos();
        CommandCompletionPosition completionPos = pos == null ? null
                : new CommandCompletionPosition(pos.getX(), pos.getY(), pos.getZ());
        this.nfrUi$clientCompletions = ClientCommandCompletionApi.resolve(
                prefix, completionPos, ClientCommandHandler.instance.latestAutoComplete);
    }

    @Inject(method = "onKeyPress", at = @At("HEAD"), cancellable = true,
            require = 1, remap = false)
    private void nfrUi$disableKeyboard(int key, CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()) cir.setReturnValue(false);
    }

    @ModifyVariable(method = "setCompletions([Ljava/lang/String;)V", at = @At("HEAD"),
            argsOnly = true, ordinal = 0, require = 1, remap = false)
    private String[] nfrUi$mergeClientCompletions(String[] serverValues) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()) return serverValues;
        this.nfrUi$mergedCompletions = CommandCompletionCandidates.merge(
                serverValues, this.nfrUi$clientCompletions);
        return this.nfrUi$mergedCompletions.plainValues();
    }

    @Inject(method = "setCompletions([Ljava/lang/String;)V", at = @At("RETURN"),
            require = 1, remap = false)
    private void nfrUi$colorCompletionSources(String[] values, CallbackInfo ci) {
        if (!EnhancedChatConfigAccess.commandCompletionEnabled()
                || this.nfrUi$mergedCompletions == null) return;
        ListIterator<String> iterator = this.completions.listIterator();
        while (iterator.hasNext()) {
            String value = iterator.next();
            iterator.set(CommandCompletionCandidates.styled(value,
                    this.nfrUi$mergedCompletions.sourceOf(value)));
        }
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
