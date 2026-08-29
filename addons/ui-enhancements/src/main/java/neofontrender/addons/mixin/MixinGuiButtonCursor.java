package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import neofontrender.addons.cursor.CursorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiButton.class)
public abstract class MixinGuiButtonCursor {
    @Shadow protected boolean hovered;
    @Shadow public boolean enabled;
    @Shadow public boolean visible;
    @Shadow public int x;
    @Shadow public int y;
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "drawButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V"))
    private void nfrUi$updateCursor(Minecraft minecraft, int mouseX, int mouseY,
                                    float partialTicks, CallbackInfo ci) {
        CursorManager.buttonDrawn(x, y, width, height, visible, enabled, hovered);
    }
}
