package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreenBook;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies Tiqian geometry to the signed-book page text rendered by 1.7.10. */
@Mixin(GuiScreenBook.class)
public abstract class MixinGuiScreenBookCjkTypography {
    private static final int BOOK_TEXT_WIDTH = 116;

    @Redirect(method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawSplitString(Ljava/lang/String;IIII)V",
                    ordinal = 1))
    private void nfrUi$drawSignedBookPage(FontRenderer font, String text, int x, int y,
                                          int width, int color) {
        CjkParagraphLayoutProvider.Layout layout = CjkTypographyRenderer.layout(
                font, text, BOOK_TEXT_WIDTH, font.FONT_HEIGHT);
        if (layout != null) {
            CjkTypographyRenderer.draw(font, layout, x, y, color, false,
                    128 / font.FONT_HEIGHT);
            return;
        }
        font.drawSplitString(text, x, y, width, color);
    }
}
