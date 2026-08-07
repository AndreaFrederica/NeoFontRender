package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.chat.ChatPlayerLinks;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
public abstract class MixinGuiScreenPlayerLinks {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void nfrUi$handlePlayerLink(int mouseX, int mouseY, int mouseButton,
                                        CallbackInfo ci) {
        if (mouseButton != 0) return;
        GuiChat chat = (GuiChat) (Object) this;
        if (chat.mc == null || !chat.mc.gameSettings.chatLinks) return;
        IChatComponent component = chat.mc.ingameGUI.getChatGUI()
                .func_146236_a(Mouse.getX(), Mouse.getY());
        String player = component == null ? null : ChatPlayerLinks.playerFrom(component);
        if (player == null) return;
        ChatPlayerLinks.activate(player);
        ci.cancel();
    }
}
