package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiCreateWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiCreateWorld.class)
public interface GuiCreateWorldAccessor {
    @Invoker("toggleMoreWorldOptions")
    void nfrUi$toggleMoreWorldOptions();

    @Invoker("showMoreWorldOptions")
    void nfrUi$showMoreWorldOptions(boolean show);
}
