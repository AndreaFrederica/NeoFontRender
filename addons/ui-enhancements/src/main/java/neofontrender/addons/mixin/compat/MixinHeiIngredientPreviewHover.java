package neofontrender.addons.mixin.compat;

import neofontrender.addons.hover.IngredientGridHoverTarget;
import neofontrender.addons.tooltips.HeiIngredientPreviewHover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Point;

/** Pinned tooltip grids have their own highlight pass, separate from IngredientGrid.draw. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.ingredients.IngredientListPreview", remap = false)
public abstract class MixinHeiIngredientPreviewHover {
    @Unique private final HeiIngredientPreviewHover nfrUi$previewHover = new HeiIngredientPreviewHover();

    @Inject(method = "drawHighlight(II)V", at = @At("HEAD"), require = 1, remap = false)
    private void nfrUi$beginPreviewHighlight(int mouseX, int mouseY, CallbackInfo ci) {
        nfrUi$previewHover.beginFrame();
    }

    @ModifyVariable(method = "drawHighlight(II)V", at = @At("STORE"), ordinal = 0,
            require = 1, remap = false)
    private Point nfrUi$capturePreviewOrigin(Point origin) {
        nfrUi$previewHover.setOrigin(origin);
        return origin;
    }

    @Redirect(method = "drawHighlight(II)V", at = @At(value = "INVOKE",
            target = "Lmezz/jei/render/IngredientRenderer;drawHighlight()V"), require = 1, remap = false)
    private void nfrUi$capturePreviewHighlight(@Coerce Object renderer) {
        nfrUi$previewHover.capture((IngredientGridHoverTarget) renderer);
    }

    // RETURN includes the no-hover early returns so the previous cell can finish fading out.
    @Inject(method = "drawHighlight(II)V", at = @At("RETURN"), require = 1, remap = false)
    private void nfrUi$drawPreviewHighlights(int mouseX, int mouseY, CallbackInfo ci) {
        nfrUi$previewHover.draw();
    }

    @Inject(method = "setScrollOffset(F)V", at = @At(value = "INVOKE",
            target = "Lmezz/jei/render/IngredientListBatchRenderer;set(ILjava/util/List;)V"),
            require = 1, remap = false)
    private void nfrUi$clearReplacedPreviewCells(float offset, CallbackInfo ci) {
        nfrUi$previewHover.clear();
    }
}
