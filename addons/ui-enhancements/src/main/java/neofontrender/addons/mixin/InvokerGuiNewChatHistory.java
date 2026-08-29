package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Inserts persisted messages without replaying GuiNewChat's normal receive path. */
@Mixin(GuiNewChat.class)
public interface InvokerGuiNewChatHistory {
    @Invoker("setChatLine")
    void nfrUi$restoreChatLine(ITextComponent component, int id, int updateCounter, boolean displayOnly);
}
