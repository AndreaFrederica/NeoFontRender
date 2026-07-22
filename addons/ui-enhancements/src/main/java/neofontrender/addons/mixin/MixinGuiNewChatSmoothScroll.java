package neofontrender.addons.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import neofontrender.addons.chat.ChatAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatSmoothScroll {
    @Shadow private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;
    @Shadow private boolean isScrolled;
    @Shadow public abstract int getLineCount();
    @Shadow public abstract float getChatScale();

    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();
    @Unique private boolean nfrUi$translated;
    @Unique private float nfrUi$fraction;

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"))
    private void nfrUi$messageAdded(net.minecraft.util.text.ITextComponent component, int id, CallbackInfo ci) {
        ChatAnimationController.messageAdded();
    }

    @Inject(method = "scroll", at = @At("HEAD"), cancellable = true)
    private void nfrUi$smoothScroll(int amount, CallbackInfo ci) {
        if (!SmoothScrollConfigAccess.chatEnabled() || amount == 0) return;
        nfrUi$scroller.scrollBy(amount, nfrUi$maxScroll(), scrollPos);
        isScrolled = nfrUi$scroller.getTarget() > 0.0F;
        ci.cancel();
    }

    @Inject(method = "resetScroll", at = @At("RETURN"))
    private void nfrUi$reset(CallbackInfo ci) {
        nfrUi$scroller.sync(0.0F);
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void nfrUi$beforeDraw(int updateCounter, CallbackInfo ci) {
        nfrUi$translated = false;
        nfrUi$fraction = 0.0F;
        if (!SmoothScrollConfigAccess.chatEnabled()) {
            nfrUi$scroller.sync(scrollPos);
            return;
        }
        float position = nfrUi$scroller.update(scrollPos, nfrUi$maxScroll());
        scrollPos = (int) Math.floor(position);
        isScrolled = position > 0.0F;
        float fraction = position - scrollPos;
        nfrUi$fraction = fraction;
        float messageOffset = ChatAnimationController.messageOffset(scrollPos != 0) * getChatScale();
        float totalOffset = fraction * 9.0F * getChatScale() + messageOffset;
        if (Math.abs(totalOffset) > 0.001F) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, totalOffset, 0.0F);
            nfrUi$translated = true;
        }
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;getLineCount()I"))
    private int nfrUi$renderEnteringLine(GuiNewChat instance) {
        return getLineCount() + (nfrUi$fraction > 0.001F ? 1 : 0);
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void nfrUi$afterDraw(int updateCounter, CallbackInfo ci) {
        if (nfrUi$translated) GlStateManager.popMatrix();
        nfrUi$translated = false;
    }

    @Unique
    private float nfrUi$maxScroll() {
        return Math.max(0, drawnChatLines.size() - getLineCount());
    }
}
