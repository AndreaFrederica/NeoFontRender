package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiOptionSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiOptionSlider.class)
public interface AccessorGuiOptionSliderNavigation {
    // 1.7.10 left the slider value in the unmapped private field field_146134_p.
    @Accessor("field_146134_p") float nfrUi$getSliderValue();
}
