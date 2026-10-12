package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.worldcreation.WorldOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Consolidates single-player difficulty and game rules under World Options.
 *
 * <p>1.7.10's options screen keeps its difficulty widget in the button list instead of a field, and
 * the difficulty lock introduced in 1.9 does not exist yet, so there is no lock widget to fold in.</p>
 */
@Mixin(GuiOptions.class)
public abstract class MixinGuiOptionsWorldOptions extends GuiScreen {
    @Unique private static final int NFR_WORLD_OPTIONS = 18741;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$addWorldOptions(CallbackInfo ci) {
        if (mc.theWorld == null || !mc.isSingleplayer() || mc.getIntegratedServer() == null) return;
        GuiButton difficulty = nfrUi$difficultyButton();
        if (difficulty == null) return;
        difficulty.visible = false;
        buttonList.add(new GuiButton(NFR_WORLD_OPTIONS, difficulty.xPosition, difficulty.yPosition,
                difficulty.width, difficulty.height,
                AddonI18n.tr("neofontrender_ui_enhancements.world_options.button")));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$openWorldOptions(GuiButton button, CallbackInfo ci) {
        if (button.id != NFR_WORLD_OPTIONS) return;
        WorldOptionsScreen.open((GuiScreen) (Object) this);
        ci.cancel();
    }

    @Unique
    private GuiButton nfrUi$difficultyButton() {
        int id = GameSettings.Options.DIFFICULTY.returnEnumOrdinal();
        for (GuiButton button : buttonList) {
            if (button.id == id) return button;
        }
        return null;
    }
}
