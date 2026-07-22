package neofontrender.mixin;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.client.input.history.VanillaTextState;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

/** Keeps backspace/delete from splitting a supplementary Unicode code point. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField {
    @Unique private static final int NFR_HISTORY_LIMIT = 100;
    @Unique private final Deque<VanillaTextState> nfr$undo = new ArrayDeque<>();
    @Unique private final Deque<VanillaTextState> nfr$redo = new ArrayDeque<>();
    @Unique private VanillaTextState nfr$beforeKey;

    @Inject(method = "textboxKeyTyped(CI)Z", at = @At("HEAD"), cancellable = true)
    private void nfr$handleUndoRedo(char typedChar, int keyCode, CallbackInfoReturnable<Boolean> cir) {
        nfr$handleUndoRedoCommon(typedChar, keyCode, cir);
    }

    @Unique
    private void nfr$handleUndoRedoCommon(char typedChar, int keyCode, CallbackInfoReturnable<Boolean> cir) {
        if (!NeofontrenderConfig.laboratoryTextUndoRedo()) return;
        boolean control = GuiScreen.isCtrlKeyDown();
        if (control && keyCode == Keyboard.KEY_Z) {
            if (GuiScreen.isShiftKeyDown()) nfr$redo(); else nfr$undo();
            cir.setReturnValue(true);
            return;
        }
        if (control && keyCode == Keyboard.KEY_Y) {
            nfr$redo();
            cir.setReturnValue(true);
            return;
        }
        nfr$beforeKey = nfr$snapshot();
    }

    @Inject(method = "textboxKeyTyped(CI)Z", at = @At("RETURN"))
    private void nfr$recordKeyboardEdit(char typedChar, int keyCode, CallbackInfoReturnable<Boolean> cir) {
        nfr$recordKeyboardEditCommon();
    }

    @Unique
    private void nfr$recordKeyboardEditCommon() {
        if (nfr$beforeKey == null) return;
        VanillaTextState before = nfr$beforeKey;
        nfr$beforeKey = null;
        if (!before.text.equals(nfr$field().getText())) {
            nfr$push(nfr$undo, before);
            nfr$redo.clear();
        }
    }

    @Unique
    private void nfr$undo() {
        if (nfr$undo.isEmpty()) return;
        nfr$push(nfr$redo, nfr$snapshot());
        nfr$restore(nfr$undo.pop());
    }

    @Unique
    private void nfr$redo() {
        if (nfr$redo.isEmpty()) return;
        nfr$push(nfr$undo, nfr$snapshot());
        nfr$restore(nfr$redo.pop());
    }

    @Unique
    private VanillaTextState nfr$snapshot() {
        GuiTextField field = nfr$field();
        return new VanillaTextState(field.getText(), field.getCursorPosition(), field.getSelectionEnd());
    }

    @Unique
    private void nfr$restore(VanillaTextState state) {
        GuiTextField field = nfr$field();
        field.setText(state.text);
        field.setCursorPosition(Math.min(state.cursor, field.getText().length()));
        field.setSelectionPos(Math.min(state.selection, field.getText().length()));
    }

    @Unique
    private GuiTextField nfr$field() {
        return (GuiTextField) (Object) this;
    }

    @Unique
    private static void nfr$push(Deque<VanillaTextState> history, VanillaTextState state) {
        history.push(state);
        while (history.size() > NFR_HISTORY_LIMIT) history.removeLast();
    }

    @ModifyVariable(method = "deleteFromCursor", at = @At("HEAD"), argsOnly = true)
    private int neofontrender$deleteWholeCodePoint(int amount) {
        if (!NeofontrenderConfig.fixUnicodeTextDeletion()) {
            return amount;
        }
        if (amount != -1 && amount != 1) {
            return amount;
        }
        GuiTextField field = (GuiTextField) (Object) this;
        String value = field.getText();
        int cursor = field.getCursorPosition();
        if (amount < 0 && cursor >= 2
                && Character.isLowSurrogate(value.charAt(cursor - 1))
                && Character.isHighSurrogate(value.charAt(cursor - 2))) {
            return -2;
        }
        if (amount > 0 && cursor + 1 < value.length()
                && Character.isHighSurrogate(value.charAt(cursor))
                && Character.isLowSurrogate(value.charAt(cursor + 1))) {
            return 2;
        }
        return amount;
    }
}
