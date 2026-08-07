package neofontrender.addons.mixin;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(GuiNewChat.class)
public interface AccessorGuiNewChatFeatures {
    @Accessor("field_146253_i")
    List<ChatLine> nfrUi$getDrawnChatLines();

    @Accessor("chatLines")
    List<ChatLine> nfrUi$getChatLines();

    @Accessor("field_146250_j")
    int nfrUi$getScrollPos();
}
