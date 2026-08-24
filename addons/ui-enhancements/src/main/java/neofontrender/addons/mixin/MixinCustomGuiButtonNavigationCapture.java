package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiButtonImage;
import net.minecraft.client.gui.GuiButtonLanguage;
import net.minecraft.client.gui.GuiButtonToggle;
import net.minecraft.client.gui.recipebook.GuiButtonRecipe;
import net.minecraft.client.gui.recipebook.GuiButtonRecipeTab;
import neofontrender.addons.navigation.vanilla.VanillaWidgetCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures vanilla buttons whose custom draw method does not call GuiButton.drawButton. */
@Mixin({GuiButtonImage.class, GuiButtonLanguage.class, GuiButtonToggle.class,
        GuiButtonRecipe.class, GuiButtonRecipeTab.class})
public abstract class MixinCustomGuiButtonNavigationCapture {
    @Inject(method = "drawButton", at = @At("HEAD"))
    private void nfrUi$captureCustomButton(Minecraft minecraft, int mouseX, int mouseY,
                                           float partialTicks, CallbackInfo ci) {
        VanillaWidgetCapture.widgetDrawn((GuiButton) (Object) this);
    }
}
