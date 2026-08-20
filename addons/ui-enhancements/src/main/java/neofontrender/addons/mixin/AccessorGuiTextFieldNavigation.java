package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiTextField.class)
public interface AccessorGuiTextFieldNavigation {
    @Accessor("isEnabled") boolean nfrUi$isEnabled();
}
