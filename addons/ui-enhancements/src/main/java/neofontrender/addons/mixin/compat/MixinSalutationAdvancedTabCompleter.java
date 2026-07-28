package neofontrender.addons.mixin.compat;

import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;
import neofontrender.addons.chat.ExternalChatCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.TabCompleter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "speiger.src.salutation.client.gui.chat.AdvancedTabCompleter", remap = false)
public abstract class MixinSalutationAdvancedTabCompleter extends TabCompleter {
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_VISIBLE = 10;

    @Shadow(remap = false) protected int offset;
    @Shadow(remap = false) public abstract void select(int index);

    private int nfrUi$panelX;
    private int nfrUi$panelY;
    private int nfrUi$panelWidth;
    private int nfrUi$panelHeight;
    private int nfrUi$visibleRows;

    protected MixinSalutationAdvancedTabCompleter(GuiTextField textField, boolean hasTargetBlock) {
        super(textField, hasTargetBlock);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfrUi$renderTabbyChatSuggestions(
            int mouseX, int mouseY, FontRenderer font, CallbackInfo ci) {
        ExternalChatCompat.InputGeometry geometry =
                ExternalChatCompat.getSalutationInput(this.textField);
        if (geometry == null) return;

        int available = this.completions.size() - this.offset;
        this.nfrUi$visibleRows = Math.max(0, Math.min(available, MAX_VISIBLE));
        if (this.nfrUi$visibleRows == 0) {
            this.nfrUi$clearPanel();
            ci.cancel();
            return;
        }

        int maxTextWidth = 0;
        for (String completion : this.completions) {
            maxTextWidth = Math.max(maxTextWidth, font.getStringWidth(completion));
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        this.nfrUi$panelWidth = Math.min(maxTextWidth + 8, minecraft.currentScreen.width);
        this.nfrUi$panelHeight = this.nfrUi$visibleRows * ROW_HEIGHT + 2;

        int cursor = Math.min(this.textField.getCursorPosition(), this.textField.getText().length());
        String beforeCursor = this.textField.getText().substring(0, Math.max(0, cursor));
        int wordStart = beforeCursor.length();
        while (wordStart > 0 && !Character.isWhitespace(beforeCursor.charAt(wordStart - 1))) {
            wordStart--;
        }
        int prefixWidth = Math.round(
                font.getStringWidth(beforeCursor.substring(0, wordStart)) * geometry.scale);
        int anchorX = geometry.x + Math.min(prefixWidth, geometry.width);
        this.nfrUi$panelX = Math.max(0,
                Math.min(anchorX, minecraft.currentScreen.width - this.nfrUi$panelWidth));
        this.nfrUi$panelY = geometry.y - this.nfrUi$panelHeight - 3;
        if (this.nfrUi$panelY < 0) {
            this.nfrUi$panelY = Math.min(minecraft.currentScreen.height - this.nfrUi$panelHeight,
                    geometry.y + geometry.height + 3);
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.nfrUi$panelX, this.nfrUi$panelY, 0.0F);
        ChatStyleRenderer.panel(this.nfrUi$panelWidth, this.nfrUi$panelHeight,
                ChatStyleConfig.inputBackground, ChatStyleConfig.border,
                minecraft.gameSettings.chatOpacity);

        int hovered = this.nfrUi$rowAt(mouseX, mouseY);
        int textColor = ChatStyleRenderer.color(
                ChatStyleConfig.text, minecraft.gameSettings.chatOpacity);
        int highlight = ChatStyleRenderer.color(
                ChatStyleConfig.hoveredTab, minecraft.gameSettings.chatOpacity);
        for (int row = 0; row < this.nfrUi$visibleRows; row++) {
            int candidate = this.offset + row;
            if (row == hovered || candidate == this.completionIdx) {
                Gui.drawRect(1, row * ROW_HEIGHT + 1,
                        this.nfrUi$panelWidth - 1, (row + 1) * ROW_HEIGHT + 1, highlight);
            }
            String value = font.trimStringToWidth(
                    this.completions.get(candidate), this.nfrUi$panelWidth - 7);
            font.drawStringWithShadow(value, 4, row * ROW_HEIGHT + 3, textColor);
        }
        GlStateManager.popMatrix();
        ci.cancel();
    }

    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfrUi$selectTabbyChatSuggestion(
            int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (ExternalChatCompat.getSalutationInput(this.textField) == null) return;
        int row = this.nfrUi$rowAt(mouseX, mouseY);
        if (row < 0) {
            cir.setReturnValue(false);
            return;
        }
        this.select(this.offset + row);
        cir.setReturnValue(true);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void nfrUi$scrollTabbyChatSuggestions(
            int mouseX, int mouseY, int scroll, CallbackInfoReturnable<Boolean> cir) {
        if (ExternalChatCompat.getSalutationInput(this.textField) == null) return;
        if (this.nfrUi$rowAt(mouseX, mouseY) < 0) {
            cir.setReturnValue(false);
            return;
        }
        this.offset = Math.max(0, Math.min(this.offset + scroll,
                Math.max(0, this.completions.size() - MAX_VISIBLE)));
        cir.setReturnValue(true);
    }

    private int nfrUi$rowAt(int mouseX, int mouseY) {
        if (this.nfrUi$visibleRows <= 0
                || mouseX < this.nfrUi$panelX
                || mouseX > this.nfrUi$panelX + this.nfrUi$panelWidth
                || mouseY < this.nfrUi$panelY + 1
                || mouseY >= this.nfrUi$panelY + this.nfrUi$panelHeight - 1) {
            return -1;
        }
        int row = (mouseY - this.nfrUi$panelY - 1) / ROW_HEIGHT;
        return row < this.nfrUi$visibleRows ? row : -1;
    }

    private void nfrUi$clearPanel() {
        this.nfrUi$panelWidth = 0;
        this.nfrUi$panelHeight = 0;
        this.nfrUi$visibleRows = 0;
    }
}
