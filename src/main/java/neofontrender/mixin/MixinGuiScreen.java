package neofontrender.mixin;

import net.minecraft.client.gui.GuiScreen;
import neofontrender.client.input.ImeInputHelper;
import neofontrender.core.config.NeofontrenderConfig;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * Intercepts keyboard input to use the full Unicode codepoint from GLFW,
 * bypassing lwjglxx's char truncation that corrupts non-BMP characters (emoji).
 *
 * When a keyboard character event arrives with a truncated char, we resolve
 * it back to the full codepoint via ImeInputHelper's map and send the correct
 * surrogate pair.
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    @Shadow
    protected abstract void keyTyped(char typedChar, int keyCode) throws IOException;

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void neofontrender$fixImeInput(CallbackInfo ci) throws IOException {
        if (!NeofontrenderConfig.fixImeInput()) return;

        char c0 = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();
        boolean keyState = Keyboard.getEventKeyState();

        long codepoint = ImeInputHelper.resolveCodepoint(c0);

        if (codepoint > 0xFFFF) {
            char high = (char) (0xD800 + ((codepoint - 0x10000) >> 10));
            char low = (char) (0xDC00 + ((codepoint - 0x10000) & 0x3FF));
            if (NeofontrenderConfig.debugImeInput()) {
                System.out.println("[ImeInput] Fixing U+" + String.format("%04X", codepoint)
                        + " -> surrogates U+" + String.format("%04X", (int) high)
                        + " U+" + String.format("%04X", (int) low)
                        + " (c0=U+" + String.format("%04X", (int) c0)
                        + " keyCode=" + keyCode + " keyState=" + keyState + ")");
            }
            this.keyTyped(high, keyCode);
            this.keyTyped(low, 0);
            ci.cancel();
            return;
        }

        // Normal character — pass through to vanilla handler
    }
}
