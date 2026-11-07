package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiOpenEvent;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps DragonCore's GUI replacement layer away from the embedded TabbyChat screen.
 *
 * DragonCore 2.6.2.9 handles GuiOpenEvent at LOW priority and, when a configured
 * {@code match} entry matches {@code GuiChat}, replaces the screen with its own
 * {@code eos.moe.dragoncore.sl}.  That replacement drops GuiChat's lifecycle while
 * UIE still renders TabbyChat through the chat HUD, so the two render paths fight
 * every frame.  Other DragonCore screens and HUD elements remain untouched.
 */
@Pseudo
@Mixin(targets = "eos.moe.dragoncore.xg", remap = false)
public abstract class MixinDragonCoreTabbyChat {
    @Inject(
            method = "ALLATORIxDEMO(Lnet/minecraftforge/client/event/GuiOpenEvent;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            // DragonCore is an optional dependency; absent or version-mismatched targets must
            // leave UIE startup unaffected.
            require = 0)
    private static void nfrUi$keepEmbeddedTabbyChat(GuiOpenEvent event, CallbackInfo ci) {
        if (event != null
                && event.getGui() instanceof GuiChat
                && EnhancedChatConfigAccess.tabbedChatEnabled()) {
            ci.cancel();
        }
    }
}
