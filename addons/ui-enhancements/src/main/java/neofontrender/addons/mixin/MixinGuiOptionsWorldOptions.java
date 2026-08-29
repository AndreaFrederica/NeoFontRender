package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLockIconButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.worldcreation.WorldOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Consolidates single-player difficulty and game rules under World Options. */
@Mixin(GuiOptions.class)
public abstract class MixinGuiOptionsWorldOptions extends GuiScreen {
    @Unique private static final int NFR_WORLD_OPTIONS = 18741;
    @Shadow private GuiButton difficultyButton;
    @Shadow private GuiLockIconButton lockButton;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$addWorldOptions(CallbackInfo ci) {
        if (mc.world == null || !mc.isSingleplayer() || mc.getIntegratedServer() == null
                || difficultyButton == null) return;
        int width = difficultyButton.width;
        if (lockButton != null) {
            lockButton.visible = false;
            width += lockButton.width;
        }
        difficultyButton.visible = false;
        addButton(new GuiButton(NFR_WORLD_OPTIONS, difficultyButton.x, difficultyButton.y,
                width, difficultyButton.height,
                AddonI18n.tr("neofontrender_ui_enhancements.world_options.button")));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$openWorldOptions(GuiButton button, CallbackInfo ci) {
        if (button.id != NFR_WORLD_OPTIONS) return;
        WorldOptionsScreen.open((GuiScreen) (Object) this);
        ci.cancel();
    }
}
