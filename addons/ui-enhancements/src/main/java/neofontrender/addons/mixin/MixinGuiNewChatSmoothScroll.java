package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatInlineLayout;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.addons.chat.VanillaChatRenderState;
import neofontrender.addons.scrolling.SmoothScrollConfigAccess;
import neofontrender.addons.scrolling.SmoothScrollController;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Animates GuiNewChat's integer line scroll position with a fractional render offset. */
@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatSmoothScroll implements VanillaChatRenderState {
    @Shadow private List<ChatLine> field_146253_i;
    @Shadow private int field_146250_j;
    @Shadow private boolean field_146251_k;
    @Shadow public abstract int func_146232_i();
    @Shadow public abstract float func_146244_h();
    @Shadow public abstract int func_146246_g();

    @Unique private final SmoothScrollController nfrUi$chatScroller = new SmoothScrollController();
    @Unique private boolean nfrUi$translated;
    @Unique private float nfrUi$fraction;
    @Unique private float nfrUi$visualOffset;

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"))
    private void nfrUi$messageAdded(IChatComponent component, int id, CallbackInfo callback) {
        ChatAnimationController.messageAdded();
    }

    @Inject(method = "scroll", at = @At("HEAD"), cancellable = true)
    private void nfrUi$smoothScroll(int amount, CallbackInfo callback) {
        if (!SmoothScrollConfigAccess.chatEnabled() || amount == 0) return;
        nfrUi$chatScroller.scrollBy(amount, nfrUi$maxScroll(), field_146250_j);
        field_146251_k = nfrUi$chatScroller.getTarget() > 0.0F;
        callback.cancel();
    }

    @Inject(method = "resetScroll", at = @At("RETURN"))
    private void nfrUi$reset(CallbackInfo callback) {
        nfrUi$chatScroller.sync(0.0F);
    }

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void nfrUi$beforeDraw(int updateCounter, CallbackInfo callback) {
        nfrUi$translated = false;
        nfrUi$fraction = 0.0F;
        nfrUi$visualOffset = 0.0F;
        if (!SmoothScrollConfigAccess.chatEnabled()) {
            nfrUi$chatScroller.sync(field_146250_j);
            return;
        }
        float position = nfrUi$chatScroller.update(field_146250_j, nfrUi$maxScroll());
        field_146250_j = (int) Math.floor(position);
        field_146251_k = position > 0.0F;
        nfrUi$fraction = position - field_146250_j;
        float messageOffset = ChatAnimationController.messageOffset(field_146250_j != 0) * func_146244_h();
        int scrollHeight = field_146250_j >= 0 && field_146250_j < field_146253_i.size()
                ? ChatInlineLayout.lineHeight(field_146253_i.get(field_146250_j),
                Minecraft.getMinecraft().fontRenderer) : 9;
        float totalOffset = nfrUi$fraction * scrollHeight * func_146244_h() + messageOffset;
        nfrUi$visualOffset = totalOffset;
        if (Math.abs(totalOffset) > 0.001F) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, totalOffset, 0.0F);
            nfrUi$translated = true;
        }
    }

    @Redirect(
            method = "drawChat",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiNewChat;func_146232_i()I"))
    private int nfrUi$renderEnteringLine(GuiNewChat chat) {
        int visible = EnhancedChatFeatures.inlineGlyphs()
                ? ChatInlineLayout.visibleLineCount(field_146253_i, field_146250_j,
                func_146246_g(), Minecraft.getMinecraft().fontRenderer) : func_146232_i();
        return visible + (nfrUi$fraction > 0.001F ? 1 : 0);
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void nfrUi$afterDraw(int updateCounter, CallbackInfo callback) {
        ChatHeadRenderer.renderVanilla(field_146253_i, field_146250_j, updateCounter,
                func_146232_i(), func_146244_h());
        ChatItemIconRenderer.renderVanilla(field_146253_i, field_146250_j, updateCounter,
                func_146232_i(), func_146244_h());
        if (nfrUi$translated) GL11.glPopMatrix();
        nfrUi$translated = false;
    }

    @Unique
    private float nfrUi$maxScroll() {
        int visible = EnhancedChatFeatures.inlineGlyphs()
                ? ChatInlineLayout.visibleLineCount(field_146253_i, field_146250_j,
                func_146246_g(), Minecraft.getMinecraft().fontRenderer) : func_146232_i();
        return Math.max(0, field_146253_i.size() - visible);
    }

    @Override
    public float nfrUi$getVisualOffset() {
        return nfrUi$visualOffset;
    }
}
