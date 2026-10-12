package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.mainmenu.LastPlayedGameManager;
import neofontrender.addons.mainmenu.LastPlayedTarget;
import neofontrender.addons.mainmenu.MainMenuConfig;
import neofontrender.addons.mainmenu.MainMenuContinueButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenuContinueGame extends GuiScreen {
    @Unique private GuiButton nfrUi$continueButton;
    @Unique private LastPlayedTarget nfrUi$continueTarget;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$addContinueGame(CallbackInfo ci) {
        nfrUi$continueButton = null;
        nfrUi$continueTarget = null;
        if (!MainMenuConfig.continueGame || mc.isDemo()
                || ((Object) this).getClass() != GuiMainMenu.class) return;
        // The exact-class guard above excludes Custom Main Menu's GuiFakeMain, which builds its
        // own copy of this button through CustomMainMenuButtonInjector.
        GuiButton singleplayer = nfrUi$button(1);
        LastPlayedTarget target = LastPlayedGameManager.INSTANCE.availableTarget();
        if (singleplayer == null || target == null) return;

        nfrUi$continueTarget = target;
        nfrUi$continueButton = MainMenuContinueButton.create(this, target,
                singleplayer.xPosition, singleplayer.yPosition - 24, singleplayer.width);
        if (nfrUi$continueButton != null) this.buttonList.add(nfrUi$continueButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$resumeLastGame(GuiButton button, CallbackInfo ci) {
        if (!MainMenuContinueButton.isContinueButton(button) || nfrUi$continueTarget == null) return;
        if (!LastPlayedGameManager.INSTANCE.resume(this, nfrUi$continueTarget)) {
            button.enabled = false;
        }
        ci.cancel();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void nfrUi$describeContinueTarget(int mouseX, int mouseY,
                                              float partialTicks, CallbackInfo ci) {
        if (nfrUi$continueButton == null || !nfrUi$continueButton.visible || nfrUi$continueTarget == null
                || mouseX < nfrUi$continueButton.xPosition
                || mouseX >= nfrUi$continueButton.xPosition + nfrUi$continueButton.width
                || mouseY < nfrUi$continueButton.yPosition
                || mouseY >= nfrUi$continueButton.yPosition + nfrUi$continueButton.height) return;
        drawHoveringText(MainMenuContinueButton.tooltip(nfrUi$continueTarget), mouseX, mouseY,
                fontRendererObj);
    }

    @Unique
    private GuiButton nfrUi$button(int id) {
        for (GuiButton button : buttonList) if (button.id == id) return button;
        return null;
    }
}
