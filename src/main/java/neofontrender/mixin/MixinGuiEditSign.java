package neofontrender.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.text.TextComponentString;
import neofontrender.core.config.NeofontrenderConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

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

    private void paste(String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) {
            return;
        }

        int line = clampLine(this.editLine);
        String current = lineText(line);
        String normalized = clipboard.replace("\r\n", "\n").replace('\r', '\n');

        for (int offset = 0; offset < normalized.length(); ) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (codePoint == '\n') {
                setLine(line, current);
                line++;
                if (line > 3) {
                    this.editLine = 3;
                    return;
                }
                current = lineText(line);
                continue;
            }

            String text = new String(Character.toChars(codePoint));
            if (!isAllowed(text)) {
                continue;
            }

            String appended = current + text;
            if (!fitsSignLine(appended)) {
                setLine(line, current);
                line++;
                if (line > 3) {
                    this.editLine = 3;
                    return;
                }
                current = lineText(line);
                appended = current + text;
                if (!fitsSignLine(appended)) {
                    continue;
                }
            }
            current = appended;
        }

        setLine(line, current);
        this.editLine = clampLine(line);
    }

    private boolean fitsSignLine(String text) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(text) <= 90;
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

    private static boolean isAllowed(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!ChatAllowedCharacters.isAllowedCharacter(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int clampLine(int line) {
        return Math.max(0, Math.min(3, line));
    }
}
