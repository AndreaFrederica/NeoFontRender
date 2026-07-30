package neofontrender.addons.mixin.compat;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.ItemsGrid;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Rectangle;

/**
 * 1.7.10 equivalent of main's JEI/HEI ingredient-grid hover mixins: NEI draws its per-slot
 * hover highlight from ItemsGridSlot#beforeDraw, which every visible slot receives each frame,
 * so the fade can be driven from a single head inject. Subclasses (item panel, bookmark grid)
 * delegate to the base implementation, so they pick the animation up as well.
 */
@Pseudo
@Mixin(targets = "codechicken.nei.ItemsGrid$ItemsGridSlot", remap = false)
public abstract class MixinNeiIngredientGridHover {
    @Shadow @Final public int slotIndex;

    @Unique private final HoverAnimationState nfrUi$hoverAnimation = new HoverAnimationState();

    @Inject(method = "beforeDraw", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void nfrUi$animatedHighlight(Rectangle4i rect, ItemsGrid.MouseContext mouseContext,
                                         CallbackInfo ci) {
        boolean hovered = mouseContext != null && mouseContext.slotIndex == slotIndex;
        if (!HoverEffectsConfigAccess.jeiIngredientGridEnabled()) {
            nfrUi$hoverAnimation.reset(hovered);
            return;
        }
        nfrUi$hoverAnimation.update(hovered, HoverEffectsConfigAccess.slotEnterMillis(),
                HoverEffectsConfigAccess.slotExitMillis());
        ci.cancel();
        if (nfrUi$hoverAnimation.isVisible()) {
            HoverEffectsRenderer.drawIngredientGridHighlight(
                    new Rectangle(rect.x, rect.y, rect.w, rect.h), nfrUi$hoverAnimation.easedProgress());
        }
    }
}
