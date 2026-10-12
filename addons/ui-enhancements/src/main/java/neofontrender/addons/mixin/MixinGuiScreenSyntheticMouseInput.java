package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.navigation.SyntheticMouseInput;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreenSyntheticMouseInput {
    @Redirect(method = "handleInput", at = @At(value = "INVOKE",
            target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false))
    private boolean nfrUi$identifySyntheticMouseEvent() {
        boolean advanced = Mouse.next();
        SyntheticMouseInput.mouseNext(advanced);
        return advanced;
    }
}
