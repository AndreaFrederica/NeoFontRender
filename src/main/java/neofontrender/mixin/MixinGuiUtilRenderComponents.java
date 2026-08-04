package neofontrender.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.linebreak.CjkComponentLineWrapper;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import neofontrender.api.text.CjkParagraphLayoutRegistry;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiUtilRenderComponents.class)
public abstract class MixinGuiUtilRenderComponents {
    @Inject(method = "splitText", at = @At("HEAD"), cancellable = true)
    private static void sfr$wrapCjkComponents(ITextComponent text, int maxWidth,
                                              FontRenderer font, boolean removeLeadingSpace,
                                              boolean forceTextColor,
                                              CallbackInfoReturnable<List<ITextComponent>> cir) {
        if (NeofontrenderConfig.fixCjkLineBreak()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            String language = minecraft == null || minecraft.getLanguageManager() == null
                    || minecraft.getLanguageManager().getCurrentLanguage() == null ? ""
                    : minecraft.getLanguageManager().getCurrentLanguage().getLanguageCode();
            List<ITextComponent> provided = CjkParagraphLayoutRegistry.splitComponents(
                    new CjkParagraphLayoutProvider.ComponentRequest(text, maxWidth,
                            font.FONT_HEIGHT, language, removeLeadingSpace,
                            forceTextColor, font::getStringWidth));
            if (provided != null) {
                cir.setReturnValue(provided);
                return;
            }
            cir.setReturnValue(CjkComponentLineWrapper.wrap(
                    text, maxWidth, font, removeLeadingSpace, forceTextColor));
        }
    }
}
