package neofontrender.mixin;

import net.minecraft.client.gui.GuiTextField;
import neofontrender.core.config.NeofontrenderConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keeps backspace/delete from splitting a supplementary Unicode code point. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField {

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
