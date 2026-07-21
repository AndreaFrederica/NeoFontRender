package neofontrender.mixin;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widgets.textfield.BaseTextFieldWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldHandler;
import neofontrender.client.input.history.ModularTextState;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Per-widget undo/redo history for ModularUI single and multiline text editors. */
@Mixin(value = BaseTextFieldWidget.class, remap = false)
public abstract class MixinModularTextField {
    @Shadow protected TextFieldHandler handler;

    @Unique private static final int NFR_HISTORY_LIMIT = 100;
    @Unique private final Deque<ModularTextState> nfr$undo = new ArrayDeque<>();
    @Unique private final Deque<ModularTextState> nfr$redo = new ArrayDeque<>();
    @Unique private ModularTextState nfr$beforeKey;
    @Unique private ModularTextState nfr$beforeMouse;

    @Inject(method = "onKeyPressed", at = @At("HEAD"), cancellable = true)
    private void nfr$handleUndoRedo(char character, int keyCode,
                                   CallbackInfoReturnable<Interactable.Result> cir) {
        if (!NeofontrenderConfig.laboratoryTextUndoRedo()) return;
        boolean control = Interactable.hasControlDown();
        if (control && keyCode == Keyboard.KEY_Z) {
            if (Interactable.hasShiftDown()) nfr$redo(); else nfr$undo();
            cir.setReturnValue(Interactable.Result.SUCCESS);
            return;
        }
        if (control && keyCode == Keyboard.KEY_Y) {
            nfr$redo();
            cir.setReturnValue(Interactable.Result.SUCCESS);
            return;
        }
        nfr$beforeKey = nfr$snapshot();
    }

    @Inject(method = "onKeyPressed", at = @At("RETURN"))
    private void nfr$recordKeyboardEdit(char character, int keyCode,
                                        CallbackInfoReturnable<Interactable.Result> cir) {
        if (nfr$beforeKey == null) return;
        ModularTextState before = nfr$beforeKey;
        nfr$beforeKey = null;
        nfr$recordIfChanged(before);
    }

    @Inject(method = "onMousePressed", at = @At("HEAD"))
    private void nfr$captureMouseEdit(int mouseButton,
                                     CallbackInfoReturnable<Interactable.Result> cir) {
        if (NeofontrenderConfig.laboratoryTextUndoRedo() && mouseButton == 1) {
            nfr$beforeMouse = nfr$snapshot();
        }
    }

    @Inject(method = "onMousePressed", at = @At("RETURN"))
    private void nfr$recordMouseEdit(int mouseButton,
                                    CallbackInfoReturnable<Interactable.Result> cir) {
        if (nfr$beforeMouse == null) return;
        ModularTextState before = nfr$beforeMouse;
        nfr$beforeMouse = null;
        nfr$recordIfChanged(before);
    }

    @Unique
    private void nfr$recordIfChanged(ModularTextState before) {
        if (!before.text.equals(this.handler.getText())) {
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
    private ModularTextState nfr$snapshot() {
        return new ModularTextState(new ArrayList<>(this.handler.getText()),
                new Point(this.handler.getMainCursor()), new Point(this.handler.getOffsetCursor()));
    }

    @Unique
    private void nfr$restore(ModularTextState state) {
        List<String> text = this.handler.getText();
        text.clear();
        text.addAll(state.text);
        if (text.isEmpty()) text.add("");
        int mainLine = Math.min(state.main.y, text.size() - 1);
        int offsetLine = Math.min(state.offset.y, text.size() - 1);
        this.handler.setMainCursor(mainLine, Math.min(state.main.x, text.get(mainLine).length()), true);
        this.handler.setOffsetCursor(offsetLine, Math.min(state.offset.x, text.get(offsetLine).length()));
        this.handler.onChanged();
    }

    @Unique
    private static void nfr$push(Deque<ModularTextState> history, ModularTextState state) {
        history.push(state);
        while (history.size() > NFR_HISTORY_LIMIT) history.removeLast();
    }
}
