package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiSlot;
import neofontrender.addons.language.LanguageListSearchAccess;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiSlot.class)
public abstract class MixinGuiSlotLanguageFavorites {
    @Shadow public int width;
    @Shadow protected int mouseX;
    @Shadow protected int mouseY;
    @Shadow public abstract int getSlotIndexFromScreenCoords(int mouseX, int mouseY);

    @Inject(method = "handleMouseInput", at = @At("HEAD"), cancellable = true)
    private void nfrUi$handleFavoriteClickOnce(CallbackInfo ci) {
        if (!((Object) this instanceof LanguageListSearchAccess)
                || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        int center = width / 2;
        if (mouseX < center + 58 || mouseX > center + 110) return;

        int slotIndex = getSlotIndexFromScreenCoords(mouseX, mouseY);
        if (((LanguageListSearchAccess) this).nfrUi$toggleFavorite(slotIndex)) {
            ci.cancel();
        }
    }
}
