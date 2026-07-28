package neofontrender.addons.mixin.compat;

import net.minecraft.client.Minecraft;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import neofontrender.addons.hover.IngredientGridHoverTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Rectangle;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.IngredientGrid", remap = false)
public abstract class MixinJeiIngredientGridHover {
    private static final String DRAW = "draw(Lnet/minecraft/client/Minecraft;II)V";

    @Shadow(remap = false) public abstract Rectangle getArea();

    @Unique private final IdentityHashMap<Rectangle, HoverAnimationState> nfrUi$hoverAnimations =
            new IdentityHashMap<>();
    @Unique private Rectangle nfrUi$currentHoverArea;
    @Unique private Rectangle nfrUi$gridArea;

    @Inject(method = DRAW, at = @At("HEAD"), require = 1, remap = false)
    private void nfrUi$beginIngredientGridFrame(Minecraft minecraft, int mouseX, int mouseY,
                                                 CallbackInfo ci) {
        Rectangle gridArea = getArea();
        if (gridArea != nfrUi$gridArea) {
            nfrUi$hoverAnimations.clear();
            nfrUi$gridArea = gridArea;
        }
        nfrUi$currentHoverArea = null;
    }

    @Redirect(method = DRAW, at = @At(value = "INVOKE",
            target = "Lmezz/jei/render/IngredientRenderer;drawHighlight()V"),
            require = 1, remap = false)
    private void nfrUi$captureIngredientHighlight(@Coerce Object renderer) {
        nfrUi$captureHighlight((IngredientGridHoverTarget) renderer);
    }

    @Redirect(method = DRAW, at = @At(value = "INVOKE",
            target = "Lmezz/jei/render/CollapsedGroupRenderer;drawHighlight()V"),
            require = 0, remap = false)
    private void nfrUi$captureCollapsedGroupHighlight(@Coerce Object renderer) {
        nfrUi$captureHighlight((IngredientGridHoverTarget) renderer);
    }

    @Inject(method = DRAW, at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableAlpha()V"),
            require = 1, remap = false)
    private void nfrUi$drawIngredientGridHighlights(Minecraft minecraft, int mouseX, int mouseY,
                                                     CallbackInfo ci) {
        if (!HoverEffectsConfigAccess.jeiIngredientGridEnabled()) {
            nfrUi$hoverAnimations.clear();
            return;
        }

        Iterator<Map.Entry<Rectangle, HoverAnimationState>> iterator =
                nfrUi$hoverAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Rectangle, HoverAnimationState> entry = iterator.next();
            boolean active = entry.getKey() == nfrUi$currentHoverArea;
            HoverAnimationState animation = entry.getValue();
            animation.update(active, HoverEffectsConfigAccess.slotEnterMillis(),
                    HoverEffectsConfigAccess.slotExitMillis());
            if (!animation.isVisible() && !active) {
                iterator.remove();
                continue;
            }
            HoverEffectsRenderer.drawIngredientGridHighlight(entry.getKey(), animation.easedProgress());
        }
    }

    @Unique
    private void nfrUi$captureHighlight(IngredientGridHoverTarget renderer) {
        if (!HoverEffectsConfigAccess.jeiIngredientGridEnabled()) {
            renderer.nfrUi$drawOriginalHighlight();
            return;
        }
        nfrUi$currentHoverArea = renderer.nfrUi$hoverArea();
        nfrUi$hoverAnimations.computeIfAbsent(nfrUi$currentHoverArea,
                ignored -> new HoverAnimationState());
    }
}
