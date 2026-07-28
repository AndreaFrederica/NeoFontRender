package neofontrender.addons.mixin.compat;

import neofontrender.addons.hover.IngredientGridHoverTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.awt.Rectangle;

@Pseudo
@Mixin(targets = "mezz.jei.render.IngredientRenderer", remap = false)
public abstract class MixinJeiIngredientRendererHover implements IngredientGridHoverTarget {
    @Override
    @Accessor(value = "area", remap = false)
    public abstract Rectangle nfrUi$hoverArea();

    @Override
    @Invoker(value = "drawHighlight", remap = false)
    public abstract void nfrUi$drawOriginalHighlight();
}
