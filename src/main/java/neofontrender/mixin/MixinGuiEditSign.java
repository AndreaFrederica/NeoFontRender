package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.TextComponentString;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.linebreak.SignPasteLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;

@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSign {

    @Shadow
    @Final
    private TileEntitySign tileSign;

    @Shadow
    private int editLine;

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void neofontrender$pasteClipboard(char typedChar, int keyCode, CallbackInfo ci) throws IOException {
        if (!NeofontrenderConfig.allowSignPaste() || !GuiScreen.isKeyComboCtrlV(keyCode)) {
            return;
        }
        paste(GuiScreen.getClipboardString());
        ci.cancel();
    }

    /**
     * Wraps the complete candidate line instead of accepting code points one at a time:
     * a color token's measured width changes once the token completes, so incremental
     * acceptance could split the token and store a line that renders literal markup.
     */
    private void paste(String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) {
            return;
        }
        int line = clampLine(this.editLine);
        List<String> produced = SignPasteLayout.layout(
                Minecraft.getMinecraft().fontRenderer, lineText(line), clipboard,
                SignPasteLayout.LINE_WIDTH, SignPasteLayout.LINE_COUNT - line);
        for (int index = 0; index < produced.size(); index++) {
            setLine(line + index, produced.get(index));
        }
        if (!produced.isEmpty()) {
            this.editLine = clampLine(line + produced.size() - 1);
        }
    }

    private String lineText(int line) {
        if (this.tileSign.signText[line] == null) {
            return "";
        }
        return this.tileSign.signText[line].getUnformattedText();
    }

    private void setLine(int line, String text) {
        this.tileSign.signText[line] = new TextComponentString(text);
    }

    private static int clampLine(int line) {
        return Math.max(0, Math.min(3, line));
    }
}
