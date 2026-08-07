package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatInlineLayout;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatTimestampDecorator;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.addons.cjk.ChatTypographyRenderer;
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
    @Shadow private List<ChatLine> field_146253_i;
    @Shadow private int field_146250_j;
    @Shadow public abstract int func_146232_i();
    @Shadow public abstract int func_146228_f();
    @Shadow public abstract int func_146246_g();
    @Shadow public abstract float func_146244_h();
    @Shadow public abstract boolean getChatOpen();

    @Unique private IChatComponent nfrUi$currentDrawComponent;

    @ModifyVariable(method = "func_146237_a", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nfrUi$decorateLine(IChatComponent component) {
        return ChatItemIconRenderer.decorate(ChatTimestampDecorator.decorate(component));
    }

    @Inject(method = "func_146237_a", at = @At("HEAD"))
    private void nfrUi$beginSenderCapture(IChatComponent component, int id, int updateCounter,
                                          boolean displayOnly, CallbackInfo ci) {
        ChatHeadResolver.beginVanillaLine(component);
    }

    @Inject(method = "func_146237_a", at = @At("RETURN"))
    private void nfrUi$endSenderCapture(IChatComponent component, int id, int updateCounter,
                                        boolean displayOnly, CallbackInfo ci) {
        ChatHeadResolver.endVanillaLine();
    }

    @Redirect(method = "func_146237_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;func_146228_f()I"))
    private int nfrUi$reserveHeadWidth(GuiNewChat instance) {
        return Math.max(1, instance.func_146228_f()
                - Math.round(ChatHeadRenderer.textOffset() * instance.func_146244_h()));
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/ChatLine;func_151461_a()Lnet/minecraft/util/IChatComponent;"))
    private IChatComponent nfrUi$captureDrawComponent(ChatLine line) {
        IChatComponent component = line.func_151461_a();
        nfrUi$currentDrawComponent = component;
        return component;
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;drawRect(IIIII)V"))
    private void nfrUi$dynamicChatBackground(int left, int top, int right, int bottom, int color) {
        if (EnhancedChatFeatures.inlineGlyphs() && left == 0 && bottom <= 0
                && top == bottom - 9) {
            int row = Math.max(0, -bottom / 9);
            Minecraft minecraft = Minecraft.getMinecraft();
            int before = ChatInlineLayout.heightBefore(field_146253_i, field_146250_j, row,
                    minecraft.fontRenderer);
            int index = field_146250_j + row;
            int height = index >= 0 && index < field_146253_i.size()
                    ? ChatInlineLayout.lineHeight(field_146253_i.get(index),
                    minecraft.fontRenderer) : 9;
            Gui.drawRect(left, -before - height, right, -before, color);
            return;
        }
        Gui.drawRect(left, top, right, bottom, color);
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I"))
    private int nfrUi$drawPositionedChatLine(FontRenderer font, String text,
                                             int x, int y, int color) {
        IChatComponent component = nfrUi$currentDrawComponent;
        nfrUi$currentDrawComponent = null;
        float drawX = x + ChatHeadRenderer.textOffset();
        float drawY = y;
        if (EnhancedChatFeatures.inlineGlyphs()) {
            int row = Math.max(0, Math.round((-y - 8.0F) / 9.0F));
            drawY = ChatInlineLayout.contentY(field_146253_i, field_146250_j, row, font);
            InlineTextLayout inline = InlineTextEngine.layout(font, text);
            if (inline.hasGlyphs()) {
                return inline.draw(font, drawX, drawY, color, true);
            }
        }
        return ChatTypographyRenderer.isPositioned(component)
                ? ChatTypographyRenderer.draw(font, component, drawX, drawY, color, true)
                : font.drawStringWithShadow(text, Math.round(drawX), Math.round(drawY), color);
    }

    @Inject(method = "func_146236_a", at = @At("HEAD"), cancellable = true)
    private void nfrUi$positionedChatHitTest(int mouseX, int mouseY,
                                             CallbackInfoReturnable<IChatComponent> cir) {
        if (!getChatOpen()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(
                minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int scaleFactor = resolution.getScaleFactor();
        float chatScale = func_146244_h();
        int panelX = (int) Math.floor((mouseX / (float) scaleFactor - 3) / chatScale);
        int localX = panelX - ChatHeadRenderer.textOffset();
        int localY = (int) Math.floor((mouseY / (float) scaleFactor - 27) / chatScale);
        int visible = Math.min(field_146253_i.size() - Math.min(field_146250_j,
                        field_146253_i.size()),
                ChatInlineLayout.visibleLineCount(field_146253_i, field_146250_j,
                        func_146246_g(), minecraft.fontRenderer));
        if (panelX < 0 || localY < 0
                || panelX > Math.floor(func_146228_f() / chatScale)
                || localY >= func_146246_g() || visible <= 0) {
            cir.setReturnValue(null);
            return;
        }
        int row = 0;
        int before = 0;
        for (; row < visible; row++) {
            int height = ChatInlineLayout.lineHeight(
                    field_146253_i.get(field_146250_j + row), minecraft.fontRenderer);
            if (localY < before + height) break;
            before += height;
        }
        if (row >= visible) {
            cir.setReturnValue(null);
            return;
        }
        int index = Math.min(field_146253_i.size() - 1, field_146250_j + row);
        if (index < 0 || index >= field_146253_i.size()) {
            cir.setReturnValue(null);
            return;
        }
        IChatComponent line = field_146253_i.get(index).func_151461_a();
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
        for (Object partObject : line) {
            IChatComponent component = (IChatComponent) partObject;
            if (!(component instanceof ChatComponentText)) continue;
            String clean = ((ChatComponentText) component).getChatComponentText_TextValue();
            right += InlineTextEngine.width(minecraft.fontRenderer, clean);
            if (right > localX) {
                cir.setReturnValue(component);
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
