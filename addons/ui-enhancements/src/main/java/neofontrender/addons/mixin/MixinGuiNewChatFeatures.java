package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatItemIconRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatFeatures {
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

    @ModifyVariable(method = "func_146237_a", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private IChatComponent nfrUi$decorateItemIcons(IChatComponent component) {
        return ChatItemIconRenderer.decorate(component);
    }

    @Redirect(method = "func_146237_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiNewChat;func_146228_f()I"))
    private int nfrUi$reserveHeadWidth(GuiNewChat instance) {
        return Math.max(1, instance.func_146228_f()
                - Math.round(ChatHeadRenderer.textOffset() * instance.func_146244_h()));
    }

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;III)I"),
            index = 1)
    private int nfrUi$offsetChatText(int x) {
        return x + ChatHeadRenderer.textOffset();
    }

    @ModifyVariable(method = "func_146236_a", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int nfrUi$offsetChatHitTest(int mouseX) {
        GuiNewChat self = (GuiNewChat) (Object) this;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(
                minecraft, minecraft.displayWidth, minecraft.displayHeight);
        return mouseX - Math.round(ChatHeadRenderer.textOffset() * self.func_146244_h()
                * resolution.getScaleFactor());
    }
}
