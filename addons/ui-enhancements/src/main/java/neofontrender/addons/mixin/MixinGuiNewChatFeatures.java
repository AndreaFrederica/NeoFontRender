package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.chat.ChatHeadRenderer;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatTimestampDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChatFeatures {
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

    @ModifyVariable(method = "getChatComponent", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int nfrUi$offsetChatHitTest(int mouseX) {
        GuiNewChat self = (GuiNewChat) (Object) this;
        int scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        return mouseX - Math.round(ChatHeadRenderer.textOffset() * self.getChatScale() * scaleFactor);
    }
}
