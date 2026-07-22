package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiTextField;
import neofontrender.addons.input.TextCursorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldCursor {
    @Shadow private int x;
    @Shadow private int y;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow private boolean visible;
    @Shadow private boolean isEnabled;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void nfrUi$updateCursor(CallbackInfo ci) {
        TextCursorManager.textFieldDrawn(x, y, width, height, visible, isEnabled);
    }
}
