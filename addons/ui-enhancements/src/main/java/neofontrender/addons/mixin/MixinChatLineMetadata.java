package neofontrender.addons.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.text.ITextComponent;
import neofontrender.addons.chat.ChatHeadLineMetadata;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatMessageMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ChatLine.class)
public abstract class MixinChatLineMetadata implements ChatHeadLineMetadata {
    @Unique private UUID nfrUi$senderId;
    @Unique private boolean nfrUi$firstFragment;
    @Unique private ChatMessageMetadata nfrUi$messageMetadata;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nfrUi$captureSender(int updateCounter, ITextComponent component, int id, CallbackInfo ci) {
        ChatHeadResolver.Capture capture = ChatHeadResolver.captureVanillaLine();
        nfrUi$senderId = capture.senderId;
        nfrUi$firstFragment = capture.firstFragment;
        nfrUi$messageMetadata = capture.metadata;
    }

    @Override public UUID nfrUi$getSenderId() { return nfrUi$senderId; }
    @Override public boolean nfrUi$isFirstFragment() { return nfrUi$firstFragment; }
    @Override public ChatMessageMetadata nfrUi$getMessageMetadata() { return nfrUi$messageMetadata; }
}
