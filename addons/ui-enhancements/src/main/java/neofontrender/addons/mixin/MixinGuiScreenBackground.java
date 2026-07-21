package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.effects.ScreenEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenBackground {
    @Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawBackground(int tint, CallbackInfo ci) {
        if (ScreenEffectsRenderer.INSTANCE.drawBackground((GuiScreen) (Object) this)) ci.cancel();
    }
}
