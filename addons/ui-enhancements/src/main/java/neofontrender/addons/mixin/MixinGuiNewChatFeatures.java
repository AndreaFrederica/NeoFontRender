package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatTimestampDecorator;
import neofontrender.addons.chat.ChatInlineLayout;
import neofontrender.addons.chat.EnhancedChatFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatFeatures {
    @Shadow private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;
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
        return GuiUtilRenderComponents.splitText(ChatItemIconRenderer.decorate(component), width,
                font, keepNewlines, forceTextColor);
    }

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"),
            index = 1)
    private float nfrUi$offsetChatText(float x) {
        return x + ChatHeadRenderer.textOffset();
    }

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"),
            index = 2)
    private float nfrUi$dynamicChatTextY(float vanillaY) {
        if (!EnhancedChatFeatures.inlineGlyphs()) return vanillaY;
        int row = Math.max(0, Math.round((-vanillaY - 8.0F) / 9.0F));
        return ChatInlineLayout.contentY(drawnChatLines, scrollPos, row,
                Minecraft.getMinecraft().fontRenderer);
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

    @ModifyVariable(method = "getChatComponent", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int nfrUi$offsetChatHitTest(int mouseX) {
        GuiNewChat self = (GuiNewChat) (Object) this;
        int scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        return mouseX - Math.round(ChatHeadRenderer.textOffset() * self.getChatScale() * scaleFactor);
    }
}
