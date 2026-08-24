package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiOptionSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiOptionSlider.class)
public interface AccessorGuiOptionSliderNavigation {
    @Accessor("sliderValue") float nfrUi$getSliderValue();
}
