package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.addons.cjk.ChatTypographyRenderer;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Applies Tiqian geometry to signed-book component lines and their click hit testing. */
@Mixin(GuiScreenBook.class)
public abstract class MixinGuiScreenBookCjkTypography {
    private static final int BOOK_TEXT_WIDTH = 116;

    @Shadow private List<ITextComponent> cachedComponents;
    @Shadow private int cachedPage;
    @Shadow private int currPage;
    @Unique private CjkParagraphLayoutProvider.Layout nfrUi$signedPageLayout;
    @Unique private boolean nfrUi$signedPageDrawn;

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$beginSignedPageDraw(int mouseX, int mouseY, float partialTicks,
                                           CallbackInfo ci) {
        nfrUi$signedPageDrawn = false;
        if (cachedPage != currPage) nfrUi$signedPageLayout = null;
    }

    @Redirect(
            method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiUtilRenderComponents;splitText(Lnet/minecraft/util/text/ITextComponent;ILnet/minecraft/client/gui/FontRenderer;ZZ)Ljava/util/List;")
    )
    private List<ITextComponent> nfrUi$layoutSignedBookPage(
            ITextComponent component, int width, FontRenderer font,
            boolean removeLeadingSpace, boolean forceTextColor) {
        nfrUi$signedPageLayout = CjkTypographyRenderer.layout(
                font, component.getFormattedText(), width, font.FONT_HEIGHT);
        List<ITextComponent> positioned = CjkTypographyRenderer.splitComponents(
                font, component, width, removeLeadingSpace, forceTextColor,
                CjkParagraphLayoutProvider.ComponentRequest.Surface.BOOK);
        return positioned != null ? positioned : GuiUtilRenderComponents.splitText(
                component, width, font, removeLeadingSpace, forceTextColor);
    }

    @Redirect(
            method = "drawScreen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
                    ordinal = 4)
    )
    private int nfrUi$drawSignedBookLine(FontRenderer font, String text, int x, int y, int color) {
        if (nfrUi$signedPageLayout != null) {
            if (!nfrUi$signedPageDrawn) {
                nfrUi$signedPageDrawn = true;
                int maxLines = 128 / font.FONT_HEIGHT;
                CjkTypographyRenderer.draw(font, nfrUi$signedPageLayout,
                        x, y, color, false, maxLines);
                return x + CjkTypographyRenderer.measuredWidth(font, nfrUi$signedPageLayout);
            }
            return x;
        }
        return font.drawString(text, x, y, color);
    }

    @Inject(method = "getClickedComponentAt", at = @At("HEAD"), cancellable = true)
    private void nfrUi$hitTestSignedBookLayout(int mouseX, int mouseY,
                                               CallbackInfoReturnable<ITextComponent> cir) {
        if (cachedComponents == null) return;
        GuiScreenBook self = (GuiScreenBook) (Object) this;
        Minecraft minecraft = self.mc;
        if (minecraft == null || minecraft.fontRenderer == null) return;
        FontRenderer fontRenderer = minecraft.fontRenderer;
        int relativeX = mouseX - (self.width - 192) / 2 - 36;
        int relativeY = mouseY - 34;
        int visibleLines = Math.min(128 / fontRenderer.FONT_HEIGHT, cachedComponents.size());
        if (relativeX < 0 || relativeX > BOOK_TEXT_WIDTH || relativeY < 0
                || relativeY >= fontRenderer.FONT_HEIGHT * visibleLines + visibleLines) return;

        int lineIndex = relativeY / fontRenderer.FONT_HEIGHT;
        if (lineIndex < 0 || lineIndex >= cachedComponents.size()) return;
        ITextComponent line = cachedComponents.get(lineIndex);
        if (ChatTypographyRenderer.isPositioned(line)) {
            cir.setReturnValue(ChatTypographyRenderer.componentAt(line, relativeX));
            return;
        }
        String formatted = line.getUnformattedText();
        CjkParagraphLayoutProvider.Layout layout = CjkTypographyRenderer.layout(
                fontRenderer, formatted, BOOK_TEXT_WIDTH, fontRenderer.FONT_HEIGHT);
        if (layout == null || layout.lines().isEmpty()) return;

        List<CjkParagraphLayoutProvider.Run> runs = layout.lines().get(0).runs();
        int rawOffset = 0;
        for (ITextComponent component : line) {
            if (!(component instanceof TextComponentString)) continue;
            String text = ((TextComponentString) component).getText();
            int rawEnd = rawOffset + text.length();
            float left = Float.POSITIVE_INFINITY;
            float right = Float.NEGATIVE_INFINITY;
            for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
                CjkParagraphLayoutProvider.Run run = runs.get(runIndex);
                if (run.rawStart() < rawEnd && run.rawEnd() > rawOffset) {
                    left = Math.min(left, run.xOffset());
                    float runRight = runIndex + 1 < runs.size()
                            ? runs.get(runIndex + 1).xOffset()
                            : run.xOffset() + fontRenderer.getStringWidth(run.formattedText());
                    right = Math.max(right, runRight);
                }
            }
            if (left != Float.POSITIVE_INFINITY && relativeX >= left && relativeX < right) {
                cir.setReturnValue(component);
                return;
            }
            rawOffset = rawEnd;
        }
        cir.setReturnValue(null);
    }
}
