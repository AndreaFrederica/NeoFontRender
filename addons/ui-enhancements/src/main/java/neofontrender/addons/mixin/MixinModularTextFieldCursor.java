package neofontrender.addons.mixin;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.widgets.textfield.BaseTextFieldWidget;
import neofontrender.addons.input.TextCursorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseTextFieldWidget.class, remap = false)
public abstract class MixinModularTextFieldCursor {
    @Inject(method = "preDraw", at = @At("HEAD"))
    private void nfrUi$updateCursor(ModularGuiContext context, boolean transformed, CallbackInfo ci) {
        if (transformed) {
            TextCursorManager.modularTextFieldDrawn(((BaseTextFieldWidget<?>) (Object) this).isHovering());
        }
    }
}
