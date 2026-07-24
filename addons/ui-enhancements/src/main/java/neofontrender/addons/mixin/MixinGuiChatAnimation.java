package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiChat;
import org.lwjgl.opengl.GL11;
import neofontrender.addons.chat.ChatAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiChatAnimation {
    @Unique private boolean nfrUi$inputTranslated;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void nfrUi$chatOpened(CallbackInfo ci) {
        ChatAnimationController.chatOpened();
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void nfrUi$chatClosed(CallbackInfo ci) {
        ChatAnimationController.chatClosed();
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$beforeInputDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        float offset = ChatAnimationController.inputOffset();
        nfrUi$inputTranslated = Math.abs(offset) > 0.001F;
        if (nfrUi$inputTranslated) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, offset, 0.0F);
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void nfrUi$afterInputDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (nfrUi$inputTranslated) GL11.glPopMatrix();
        nfrUi$inputTranslated = false;
    }
}
