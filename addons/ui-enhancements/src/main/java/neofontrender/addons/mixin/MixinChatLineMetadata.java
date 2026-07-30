package neofontrender.addons.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.IChatComponent;
import neofontrender.addons.chat.ChatHeadLineMetadata;
import neofontrender.addons.chat.ChatHeadResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatLine.class)
public abstract class MixinChatLineMetadata implements ChatHeadLineMetadata {
    @Unique private String nfrUi$senderName;
    @Unique private boolean nfrUi$firstFragment;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nfrUi$captureSender(int updateCounter, IChatComponent component, int id, CallbackInfo ci) {
        ChatHeadResolver.Capture capture = ChatHeadResolver.captureVanillaLine();
        nfrUi$senderName = capture.senderName;
        nfrUi$firstFragment = capture.firstFragment;
    }

    @Override public String nfrUi$getSenderName() { return nfrUi$senderName; }
    @Override public boolean nfrUi$isFirstFragment() { return nfrUi$firstFragment; }
}
