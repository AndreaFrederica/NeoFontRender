package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiChat.class)
public interface AccessorGuiChatFeatures {
    @Accessor("inputField")
    GuiTextField nfrUi$getInputField();
}
