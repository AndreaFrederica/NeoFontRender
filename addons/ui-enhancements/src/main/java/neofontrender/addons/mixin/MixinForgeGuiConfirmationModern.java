package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.client.GuiConfirmation;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves the modern loading presentation only when a Forge query is accepted. */
@Mixin(value = GuiConfirmation.class, remap = false)
public abstract class MixinForgeGuiConfirmationModern {
    @Inject(method = "actionPerformed", remap = true, at = @At("HEAD"))
    private void nfrUi$recordLoadingConfirmation(GuiButton button, CallbackInfo ci) {
        if (!button.enabled || (button.id != 0 && button.id != 1)) return;
        if (!WorldLoadingRenderer.INSTANCE.shouldModernizeCurrentPrompt()) return;
        WorldLoadingRenderer.INSTANCE.answerLoadingPrompt(button.id == 0);
    }
}
