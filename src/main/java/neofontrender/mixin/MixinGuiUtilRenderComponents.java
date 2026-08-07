package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IChatComponent;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import neofontrender.api.text.CjkParagraphLayoutRegistry;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.linebreak.CjkComponentLineWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.GuiUtilRenderComponents")
public abstract class MixinGuiUtilRenderComponents {
    @Inject(method = "splitText", at = @At("HEAD"), cancellable = true)
    private static void sfr$wrapCjkComponents(IChatComponent text, int maxWidth,
                                              FontRenderer font, boolean removeLeadingSpace,
                                              boolean forceTextColor,
                                              CallbackInfoReturnable<List<IChatComponent>> cir) {
        if (NeofontrenderConfig.fixCjkLineBreak()) {
            Minecraft minecraft = Minecraft.getMinecraft();
            String language = minecraft == null || minecraft.getLanguageManager() == null
                    || minecraft.getLanguageManager().getCurrentLanguage() == null ? ""
                    : minecraft.getLanguageManager().getCurrentLanguage().getLanguageCode();
            List<IChatComponent> provided = CjkParagraphLayoutRegistry.splitComponents(
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
