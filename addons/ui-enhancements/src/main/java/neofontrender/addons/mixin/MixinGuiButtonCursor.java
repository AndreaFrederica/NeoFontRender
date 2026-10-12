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
    @Shadow protected boolean field_146123_n;
    @Shadow public boolean enabled;
    @Shadow public boolean visible;
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "drawButton", at = @At("RETURN"))
    private void nfrUi$updateCursor(Minecraft minecraft, int mouseX, int mouseY, CallbackInfo ci) {
        CursorManager.buttonDrawn(xPosition, yPosition, width, height, visible, enabled,
                field_146123_n);
    }
}
