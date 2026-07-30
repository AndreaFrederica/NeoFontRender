package neofontrender.addons.mixin;

import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemSlot.class, remap = false)
public abstract class MixinModularItemSlotHover {
    @Unique private final HoverAnimationState nfrUi$slotHoverAnimation = new HoverAnimationState();

    @Redirect(method = "drawOverlay()V", at = @At(value = "INVOKE",
            target = "Lcom/cleanroommc/modularui/widgets/slot/ItemSlot;isHovering()Z"))
    private boolean nfrUi$keepOverlayDuringFadeOut(ItemSlot slot) {
        boolean hovered = slot.isHovering();
        if (!HoverEffectsConfigAccess.modularUiSlotsEnabled()) {
            nfrUi$slotHoverAnimation.reset(hovered);
            return hovered;
        }
        nfrUi$slotHoverAnimation.update(hovered, HoverEffectsConfigAccess.slotEnterMillis(),
                HoverEffectsConfigAccess.slotExitMillis());
        return nfrUi$slotHoverAnimation.isVisible();
    }

    @ModifyArg(method = "drawOverlay()V", at = @At(value = "INVOKE",
            target = "Lcom/cleanroommc/modularui/drawable/GuiDraw;drawRect(FFFFI)V"), index = 4)
    private int nfrUi$fadeModularSlotColor(int themeColor) {
        if (!HoverEffectsConfigAccess.modularUiSlotsEnabled()) return themeColor;
        int baseColor = HoverEffectsConfigAccess.modularUiThemeColor()
                ? themeColor : HoverEffectsConfigAccess.slotColor();
        return HoverEffectsRenderer.multiplyAlpha(baseColor, nfrUi$slotHoverAnimation.easedProgress());
    }
}
