package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiTextField;
import neofontrender.addons.input.TextCursorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reports vanilla text field bounds to the native cursor manager. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextFieldCursor {
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow private boolean visible;
    @Shadow private boolean isEnabled;

    @Inject(method = "drawTextBox", at = @At("HEAD"))
    private void nfrUi$trackCursor(CallbackInfo callback) {
        TextCursorManager.textFieldDrawn(xPosition, yPosition, width, height, visible, isEnabled);
    }
}
