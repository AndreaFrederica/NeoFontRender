package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatTimestampDecorator;
import neofontrender.addons.chat.ChatInlineLayout;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.cjk.ChatTypographyRenderer;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.CjkParagraphLayoutProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatFeatures {
    @Shadow private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;
    @Shadow public abstract int getLineCount();
    @Shadow public abstract int getChatWidth();
    @Shadow public abstract int getChatHeight();
    @Shadow public abstract float getChatScale();
    @Shadow public abstract boolean getChatOpen();

    @Unique private ITextComponent nfrUi$currentDrawComponent;
    @ModifyVariable(method = "setChatLine", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ITextComponent nfrUi$addTimestamp(ITextComponent component) {
        return ChatTimestampDecorator.decorate(component);
    }

    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void nfrUi$beginSenderCapture(ITextComponent component, int id, int updateCounter,
                                          boolean displayOnly, CallbackInfo ci) {
        ChatHeadResolver.beginVanillaLine(component);
    }

    @Inject(method = "setChatLine", at = @At("RETURN"))
    private void nfrUi$endSenderCapture(ITextComponent component, int id, int updateCounter,
                                        boolean displayOnly, CallbackInfo ci) {
        ChatHeadResolver.endVanillaLine();
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;getChatWidth()I"))
    private int nfrUi$reserveHeadWidth(GuiNewChat instance) {
        return Math.max(1, instance.getChatWidth()
                - Math.round(ChatHeadRenderer.textOffset() * instance.getChatScale()));
    }

    @Redirect(method = "setChatLine", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiUtilRenderComponents;splitText(Lnet/minecraft/util/text/ITextComponent;ILnet/minecraft/client/gui/FontRenderer;ZZ)Ljava/util/List;"))
    private List<ITextComponent> nfrUi$decorateItemIcons(ITextComponent component, int width,
                                                         FontRenderer font, boolean keepNewlines,
                                                         boolean forceTextColor) {
        ITextComponent decorated = ChatItemIconRenderer.decorate(component);
        List<ITextComponent> positioned = CjkTypographyRenderer.splitComponents(
                font, decorated, width, keepNewlines, forceTextColor,
                CjkParagraphLayoutProvider.ComponentRequest.Surface.CHAT);
        return positioned != null ? positioned : GuiUtilRenderComponents.splitText(
                decorated, width, font, keepNewlines, forceTextColor);
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/ChatLine;getChatComponent()Lnet/minecraft/util/text/ITextComponent;"))
    private ITextComponent nfrUi$captureDrawComponent(ChatLine line) {
        ITextComponent component = line.getChatComponent();
        nfrUi$currentDrawComponent = component;
        return component;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V"), require = 0)
    private void nfrUi$dynamicChatBackground(int left, int top, int right, int bottom, int color) {
        if (EnhancedChatFeatures.inlineGlyphs() && left == -2 && bottom <= 0 && top == bottom - 9) {
            int row = Math.max(0, -bottom / 9);
            int before = ChatInlineLayout.heightBefore(drawnChatLines, scrollPos, row,
                    Minecraft.getMinecraft().fontRenderer);
            int index = scrollPos + row;
            int height = index >= 0 && index < drawnChatLines.size()
                    ? ChatInlineLayout.lineHeight(drawnChatLines.get(index),
                    Minecraft.getMinecraft().fontRenderer) : 9;
            Gui.drawRect(left, -before - height, right, -before, color);
            return;
        }
        Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int nfrUi$drawPositionedChatLine(FontRenderer font, String text,
                                             float x, float y, int color) {
        ITextComponent component = nfrUi$currentDrawComponent;
        nfrUi$currentDrawComponent = null;
        float drawX = x + ChatHeadRenderer.textOffset();
        float drawY = y;
        if (EnhancedChatFeatures.inlineGlyphs()) {
            int row = Math.max(0, Math.round((-y - 8.0F) / 9.0F));
            drawY = ChatInlineLayout.contentY(drawnChatLines, scrollPos, row, font);
            InlineTextLayout inline = InlineTextEngine.layout(font, text);
            if (inline.hasGlyphs()) {
                return inline.draw(font, drawX, drawY, color, true);
            }
        }
        return ChatTypographyRenderer.isPositioned(component)
                ? ChatTypographyRenderer.draw(font, component, drawX, drawY, color, true)
                : font.drawStringWithShadow(text, drawX, drawY, color);
    }

    @Inject(method = "getChatComponent", at = @At("HEAD"), cancellable = true)
    private void nfrUi$positionedChatHitTest(int mouseX, int mouseY,
                                             CallbackInfoReturnable<ITextComponent> cir) {
        if (!getChatOpen()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int scaleFactor = resolution.getScaleFactor();
        float chatScale = getChatScale();
        int panelX = (int) Math.floor((mouseX / scaleFactor - 2) / chatScale);
        int localX = panelX - ChatHeadRenderer.textOffset();
        int localY = (int) Math.floor((mouseY / scaleFactor - 40) / chatScale);
        int visible = Math.min(drawnChatLines.size() - Math.min(scrollPos, drawnChatLines.size()),
                ChatInlineLayout.visibleLineCount(drawnChatLines, scrollPos,
                        getChatHeight(), minecraft.fontRenderer));
        if (panelX < 0 || localY < 0
                || panelX > Math.floor(getChatWidth() / chatScale)
                || localY >= getChatHeight() || visible <= 0) {
            cir.setReturnValue(null);
            return;
        }
        int row = 0;
        int before = 0;
        for (; row < visible; row++) {
            int height = ChatInlineLayout.lineHeight(
                    drawnChatLines.get(scrollPos + row), minecraft.fontRenderer);
            if (localY < before + height) break;
            before += height;
        }
        if (row >= visible) {
            cir.setReturnValue(null);
            return;
        }
        int index = Math.min(drawnChatLines.size() - 1, scrollPos + row);
        if (index < 0 || index >= drawnChatLines.size()) {
            cir.setReturnValue(null);
            return;
        }
        ITextComponent line = drawnChatLines.get(index).getChatComponent();
        if (localX < 0) {
            cir.setReturnValue(null);
            return;
        }
        InlineTextLayout inline = InlineTextEngine.layout(
                minecraft.fontRenderer, line.getFormattedText());
        if (ChatTypographyRenderer.isPositioned(line) && !inline.hasGlyphs()) {
            cir.setReturnValue(ChatTypographyRenderer.componentAt(line, localX));
            return;
        }
        int right = 0;
        for (ITextComponent component : line) {
            if (!(component instanceof TextComponentString)) continue;
            String clean = GuiUtilRenderComponents.removeTextColorsIfConfigured(
                    ((TextComponentString) component).getText(), false);
            right += InlineTextEngine.width(minecraft.fontRenderer, clean);
            if (right > localX) {
                cir.setReturnValue(component);
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
