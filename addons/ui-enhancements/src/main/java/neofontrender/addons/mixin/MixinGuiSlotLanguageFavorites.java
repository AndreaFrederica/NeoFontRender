package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiSlot;
import neofontrender.addons.language.LanguageListSearchAccess;
import org.lwjglx.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.10's GuiSlot polls clicks inside drawScreen instead of handleMouseInput. A press on the
 * favorite region is consumed by claiming it as the drag origin, which skips elementClicked for
 * exactly that click and keeps later dragging behavior identical to a normal slot press.
 */
@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotLanguageFavorites {
    @Shadow public int width;
    @Shadow public int left;
    @Shadow public int right;
    @Shadow public int top;
    @Shadow public int bottom;
    @Shadow private float initialClickY;
    @Shadow private float scrollMultiplier;
    @Shadow public abstract boolean func_148125_i();
    @Shadow public abstract int func_148124_c(int mouseX, int mouseY);

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void nfrUi$handleFavoriteClickOnce(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!((Object) this instanceof LanguageListSearchAccess)
                || !Mouse.isButtonDown(0) || !func_148125_i() || initialClickY != -1.0F) return;
        if (mouseX <= left || mouseX >= right || mouseY < top || mouseY > bottom) return;
        int center = width / 2;
        if (mouseX < center + 58 || mouseX > center + 110) return;

        int slotIndex = func_148124_c(mouseX, mouseY);
        if (((LanguageListSearchAccess) this).nfrUi$toggleFavorite(slotIndex)) {
            scrollMultiplier = 1.0F;
            initialClickY = mouseY;
        }
    }
}
