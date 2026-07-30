package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.mainmenu.LastPlayedGameManager;
import neofontrender.addons.mainmenu.LastPlayedTarget;
import neofontrender.addons.mainmenu.MainMenuConfig;
import neofontrender.addons.tooltips.AddonI18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenuContinueGame extends GuiScreen {
    @Unique private static final int NFR_CONTINUE_GAME = 28642;
    @Unique private GuiButton nfrUi$continueButton;
    @Unique private LastPlayedTarget nfrUi$continueTarget;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$addContinueGame(CallbackInfo ci) {
        nfrUi$continueButton = null;
        nfrUi$continueTarget = null;
        if (!MainMenuConfig.continueGame || mc.isDemo()
                || ((Object) this).getClass() != GuiMainMenu.class) return;
        GuiButton singleplayer = nfrUi$button(1);
        LastPlayedTarget target = LastPlayedGameManager.INSTANCE.availableTarget();
        if (singleplayer == null || target == null) return;

        nfrUi$continueTarget = target;
        String fullLabel = AddonI18n.tr(
                "neofontrender_ui_enhancements.main_menu.continue_game")
                + ": " + target.displayName();
        String label = nfrUi$fit(fullLabel, singleplayer.width - 8);
        nfrUi$continueButton = new GuiButton(NFR_CONTINUE_GAME,
                singleplayer.xPosition, singleplayer.yPosition - 24, singleplayer.width, 20, label);
        this.buttonList.add(nfrUi$continueButton);
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$resumeLastGame(GuiButton button, CallbackInfo ci) {
        if (button.id != NFR_CONTINUE_GAME || nfrUi$continueTarget == null) return;
        if (!LastPlayedGameManager.INSTANCE.resume(this, nfrUi$continueTarget)) {
            button.enabled = false;
        }
        ci.cancel();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void nfrUi$describeContinueTarget(int mouseX, int mouseY,
                                              float partialTicks, CallbackInfo ci) {
        if (nfrUi$continueButton == null || nfrUi$continueTarget == null
                || mouseX < nfrUi$continueButton.xPosition
                || mouseX >= nfrUi$continueButton.xPosition + nfrUi$continueButton.width
                || mouseY < nfrUi$continueButton.yPosition
                || mouseY >= nfrUi$continueButton.yPosition + nfrUi$continueButton.height) return;
        String type = nfrUi$continueTarget.kind() == LastPlayedTarget.Kind.SINGLEPLAYER
                ? AddonI18n.tr("neofontrender_ui_enhancements.main_menu.singleplayer")
                : AddonI18n.tr("neofontrender_ui_enhancements.main_menu.server");
        String detailLabel = nfrUi$continueTarget.kind() == LastPlayedTarget.Kind.SINGLEPLAYER
                ? AddonI18n.tr("neofontrender_ui_enhancements.main_menu.folder")
                : AddonI18n.tr("neofontrender_ui_enhancements.main_menu.address");
        String detail = nfrUi$continueTarget.kind() == LastPlayedTarget.Kind.SINGLEPLAYER
                ? nfrUi$continueTarget.identifier() : nfrUi$continueTarget.address();
        drawHoveringText(Arrays.asList(type + ": " + nfrUi$continueTarget.displayName(),
                detailLabel + ": " + detail), mouseX, mouseY, fontRendererObj);
    }

    @Unique
    private GuiButton nfrUi$button(int id) {
        for (GuiButton button : buttonList) if (button.id == id) return button;
        return null;
    }

    @Unique
    private String nfrUi$fit(String text, int maximumWidth) {
        if (fontRendererObj.getStringWidth(text) <= maximumWidth) return text;
        String suffix = "...";
        return fontRendererObj.trimStringToWidth(text,
                Math.max(0, maximumWidth - fontRendererObj.getStringWidth(suffix))) + suffix;
    }
}
