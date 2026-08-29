package neofontrender.addons.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.VanillaChatRenderState;
import neofontrender.addons.chat.ChatInlineLayout;
import neofontrender.addons.chat.EnhancedChatFeatures;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;
import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatSmoothScroll implements VanillaChatRenderState {
    @Shadow private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;
    @Shadow private boolean isScrolled;
    @Shadow public abstract int getLineCount();
    @Shadow public abstract float getChatScale();
    @Shadow public abstract int getChatHeight();
    @Shadow public abstract int getChatWidth();

    @Unique private final SmoothScrollController nfrUi$scroller = new SmoothScrollController();
    @Unique private boolean nfrUi$translated;
    @Unique private float nfrUi$fraction;
    @Unique private float nfrUi$visualOffset;
    @Unique private boolean nfrUi$ownsScrollPos;
    @Unique private int nfrUi$appliedScrollPos;
    @Unique private final IntBuffer nfrUi$oldScissor = BufferUtils.createIntBuffer(4);
    @Unique private boolean nfrUi$clipActive;
    @Unique private boolean nfrUi$scissorWasEnabled;

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"))
    private void nfrUi$messageAdded(net.minecraft.util.text.ITextComponent component, int id, CallbackInfo ci) {
        ChatAnimationController.messageAdded();
    }

    @Inject(method = "scroll", at = @At("HEAD"), cancellable = true)
    private void nfrUi$smoothScroll(int amount, CallbackInfo ci) {
        if (!SmoothScrollConfigAccess.chatEnabled() || amount == 0) return;
        if (!nfrUi$ownsScrollPos || scrollPos != nfrUi$appliedScrollPos) {
            nfrUi$scroller.sync(scrollPos);
        }
        nfrUi$scroller.scrollBy(amount, nfrUi$maxScroll(), scrollPos);
        isScrolled = nfrUi$scroller.getTarget() > 0.0F;
        nfrUi$appliedScrollPos = scrollPos;
        nfrUi$ownsScrollPos = true;
        ci.cancel();
    }

    @Inject(method = "resetScroll", at = @At("RETURN"))
    private void nfrUi$reset(CallbackInfo ci) {
        nfrUi$scroller.sync(0.0F);
        nfrUi$appliedScrollPos = 0;
        nfrUi$ownsScrollPos = true;
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void nfrUi$beforeDraw(int updateCounter, CallbackInfo ci) {
        nfrUi$beginClip();
        nfrUi$translated = false;
        nfrUi$fraction = 0.0F;
        nfrUi$visualOffset = 0.0F;
        if (!SmoothScrollConfigAccess.chatEnabled()) {
            nfrUi$scroller.sync(scrollPos);
            nfrUi$ownsScrollPos = false;
            return;
        }
        if (!nfrUi$ownsScrollPos || scrollPos != nfrUi$appliedScrollPos) {
            nfrUi$scroller.sync(scrollPos);
        }
        float position = nfrUi$scroller.updateOwned(nfrUi$maxScroll());
        scrollPos = (int) Math.floor(position);
        nfrUi$appliedScrollPos = scrollPos;
        nfrUi$ownsScrollPos = true;
        isScrolled = position > 0.0F;
        float fraction = position - scrollPos;
        nfrUi$fraction = fraction;
        float messageOffset = ChatAnimationController.messageOffset(scrollPos != 0) * getChatScale();
        int scrollHeight = scrollPos >= 0 && scrollPos < drawnChatLines.size()
                ? ChatInlineLayout.lineHeight(drawnChatLines.get(scrollPos),
                Minecraft.getMinecraft().fontRenderer) : 9;
        float totalOffset = fraction * scrollHeight * getChatScale() + messageOffset;
        nfrUi$visualOffset = totalOffset;
        if (Math.abs(totalOffset) > 0.001F) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, totalOffset, 0.0F);
            nfrUi$translated = true;
        }
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;getLineCount()I"))
    private int nfrUi$renderEnteringLine(GuiNewChat instance) {
        return nfrUi$renderLineCount();
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void nfrUi$afterDraw(int updateCounter, CallbackInfo ci) {
        int renderLines = nfrUi$renderLineCount();
        ChatHeadRenderer.renderVanilla(drawnChatLines, scrollPos, updateCounter,
                renderLines, getChatScale());
        ChatItemIconRenderer.renderVanilla(drawnChatLines, scrollPos, updateCounter,
                renderLines, getChatScale());
        if (nfrUi$translated) GlStateManager.popMatrix();
        nfrUi$translated = false;
        nfrUi$endClip();
    }

    @Unique
    private int nfrUi$renderLineCount() {
        int visible = EnhancedChatFeatures.inlineGlyphs()
                ? ChatInlineLayout.visibleLineCount(drawnChatLines, scrollPos, getChatHeight(),
                Minecraft.getMinecraft().fontRenderer) : getLineCount();
        return visible + (nfrUi$fraction > 0.001F ? 1 : 0);
    }

    @Unique
    private float nfrUi$maxScroll() {
        if (EnhancedChatFeatures.inlineGlyphs()) {
            return ChatInlineLayout.maximumScrollIndex(drawnChatLines, getChatHeight(),
                    Minecraft.getMinecraft().fontRenderer);
        }
        return Math.max(0, drawnChatLines.size() - getLineCount());
    }

    @Unique
    private void nfrUi$beginClip() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) return;
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int factor = resolution.getScaleFactor();
        int viewportBottom = resolution.getScaledHeight() - 40;
        int viewportTop = viewportBottom - Math.max(1, getChatHeight());
        int x = 0;
        int y = minecraft.displayHeight - viewportBottom * factor;
        int width = Math.min(minecraft.displayWidth, Math.max(1, getChatWidth() + 6) * factor);
        int height = Math.min(minecraft.displayHeight - Math.max(0, y),
                Math.max(1, viewportBottom - viewportTop) * factor);
        y = Math.max(0, y);
        if (width <= 0 || height <= 0) return;

        nfrUi$scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (nfrUi$scissorWasEnabled) {
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, nfrUi$oldScissor);
            int oldX = nfrUi$oldScissor.get(0);
            int oldY = nfrUi$oldScissor.get(1);
            int right = Math.min(x + width, oldX + nfrUi$oldScissor.get(2));
            int top = Math.min(y + height, oldY + nfrUi$oldScissor.get(3));
            x = Math.max(x, oldX);
            y = Math.max(y, oldY);
            width = Math.max(0, right - x);
            height = Math.max(0, top - y);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
        nfrUi$clipActive = true;
    }

    @Unique
    private void nfrUi$endClip() {
        if (!nfrUi$clipActive) return;
        if (nfrUi$scissorWasEnabled) {
            GL11.glScissor(nfrUi$oldScissor.get(0), nfrUi$oldScissor.get(1),
                    nfrUi$oldScissor.get(2), nfrUi$oldScissor.get(3));
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        nfrUi$clipActive = false;
    }

    @Override
    public float nfrUi$getVisualOffset() {
        return nfrUi$visualOffset;
    }
}
