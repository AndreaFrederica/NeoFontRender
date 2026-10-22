package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.mainmenu.LastPlayedGameManager;
import neofontrender.addons.mainmenu.LastPlayedTarget;
import neofontrender.addons.mainmenu.MainMenuConfig;
import neofontrender.addons.mainmenu.MainMenuContinueButton;
import neofontrender.client.NeofontrenderFancyMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

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
        // FancyMenu caches this vanilla button after initGui, so keep this route for its layouts.
        // The exact-class guard above excludes CMM's GuiFakeMain.
        GuiButton singleplayer = nfrUi$button(1);
        LastPlayedTarget target = LastPlayedGameManager.INSTANCE.availableTarget();
        if (singleplayer == null || target == null) return;

        nfrUi$continueTarget = target;
        nfrUi$continueButton = MainMenuContinueButton.create(this, target,
                singleplayer.x, singleplayer.y - 24, singleplayer.width);
        if (nfrUi$continueButton != null) addButton(nfrUi$continueButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$resumeLastGame(GuiButton button, CallbackInfo ci) throws IOException {
        if (!MainMenuContinueButton.isContinueButton(button) || nfrUi$continueTarget == null) return;
        if (!LastPlayedGameManager.INSTANCE.resume(this, nfrUi$continueTarget)) {
            button.enabled = false;
        }
        ci.cancel();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void nfrUi$describeContinueTarget(int mouseX, int mouseY,
                                              float partialTicks, CallbackInfo ci) {
        if (NeofontrenderFancyMenu.ownsScreen(this)) return;
        if (nfrUi$continueButton == null || !nfrUi$continueButton.visible || nfrUi$continueTarget == null
                || mouseX < nfrUi$continueButton.x
                || mouseX >= nfrUi$continueButton.x + nfrUi$continueButton.width
                || mouseY < nfrUi$continueButton.y
                || mouseY >= nfrUi$continueButton.y + nfrUi$continueButton.height) return;
        drawHoveringText(MainMenuContinueButton.tooltip(nfrUi$continueTarget), mouseX, mouseY);
    }

    @Unique
    private GuiButton nfrUi$button(int id) {
        for (GuiButton button : buttonList) if (button.id == id) return button;
        return null;
    }
}
