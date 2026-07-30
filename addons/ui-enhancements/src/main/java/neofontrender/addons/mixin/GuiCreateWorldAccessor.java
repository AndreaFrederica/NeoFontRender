package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiCreateWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiCreateWorld.class)
public interface GuiCreateWorldAccessor {
    /** 1.7.10 name of the private toggleMoreWorldOptions method. */
    @Invoker("func_146315_i")
    void nfrUi$toggleMoreWorldOptions();
}
