package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.GuiNotification;
import net.minecraftforge.fml.common.StartupQuery;
import neofontrender.addons.loading.ModernLoadingPromptButton;
import neofontrender.addons.loading.ModernLoadingPromptRenderer;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(value = GuiNotification.class, remap = false)
public abstract class MixinForgeGuiNotificationModern extends GuiScreen {
    @Shadow @Final protected StartupQuery query;
    @Unique private int nfrUi$promptScroll;

    @Inject(method = "initGui", remap = true, at = @At("RETURN"))
    private void nfrUi$modernPromptButtons(CallbackInfo ci) {
        if (!WorldLoadingRenderer.INSTANCE.shouldModernizeCurrentPrompt()) return;
        boolean confirmation = query.getResult() != null;
        ModernLoadingPromptRenderer.Layout layout = ModernLoadingPromptRenderer.layout(
                fontRenderer, query.getText(), width, height);
        nfrUi$installModernButtons(layout, confirmation);
    }

    @Inject(method = "actionPerformed", remap = true, at = @At("HEAD"))
    private void nfrUi$continueAfterNotification(GuiButton button, CallbackInfo ci) {
        if (button.enabled && button.id == 0
                && WorldLoadingRenderer.INSTANCE.shouldModernizeCurrentPrompt()) {
            WorldLoadingRenderer.INSTANCE.answerLoadingPrompt(true);
        }
    }

    @Unique
    private void nfrUi$installModernButtons(ModernLoadingPromptRenderer.Layout layout,
                                            boolean confirmation) {
        buttonList.clear();
        if (confirmation) {
            int buttonWidth = Math.min(150, Math.max(80, (layout.width - 38) / 2));
            int gap = 10;
            int left = layout.left + (layout.width - buttonWidth * 2 - gap) / 2;
            buttonList.add(new ModernLoadingPromptButton(0, left, layout.buttonY(),
                    buttonWidth, I18n.format("gui.yes"), true));
            buttonList.add(new ModernLoadingPromptButton(1, left + buttonWidth + gap,
                    layout.buttonY(), buttonWidth, I18n.format("gui.no"), false));
        } else {
            int buttonWidth = Math.min(180, layout.width - 28);
            buttonList.add(new ModernLoadingPromptButton(0,
                    layout.left + (layout.width - buttonWidth) / 2, layout.buttonY(),
                    buttonWidth, I18n.format("gui.done"), true));
        }
    }

    @Inject(method = "drawScreen", remap = true, at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawModernPrompt(int mouseX, int mouseY, float partialTicks,
                                        CallbackInfo ci) {
        if (!WorldLoadingRenderer.INSTANCE.shouldModernizeCurrentPrompt()) return;
        ModernLoadingPromptRenderer.Layout layout = ModernLoadingPromptRenderer.layout(
                fontRenderer, query.getText(), width, height);
        if (buttonList.isEmpty() || !(buttonList.get(0) instanceof ModernLoadingPromptButton)) {
            nfrUi$installModernButtons(layout, query.getResult() != null);
        }
        nfrUi$promptScroll = Math.max(0, Math.min(layout.maxScroll(), nfrUi$promptScroll));
        ModernLoadingPromptRenderer.draw(fontRenderer, query.getText(),
                query.getResult() != null, width, height, nfrUi$promptScroll);
        for (GuiButton button : buttonList) button.drawButton(mc, mouseX, mouseY, partialTicks);
        ci.cancel();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (!WorldLoadingRenderer.INSTANCE.shouldModernizeCurrentPrompt()) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        ModernLoadingPromptRenderer.Layout layout = ModernLoadingPromptRenderer.layout(
                fontRenderer, query.getText(), width, height);
        int direction = wheel > 0 ? -3 : 3;
        nfrUi$promptScroll = Math.max(0,
                Math.min(layout.maxScroll(), nfrUi$promptScroll + direction));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // GuiScreen's Escape behavior closes the screen without completing StartupQuery, which
        // leaves the integrated-server thread waiting forever. The explicit buttons own dismissal.
        if (keyCode == 1 && WorldLoadingRenderer.INSTANCE.shouldModernizeCurrentPrompt()) return;
        super.keyTyped(typedChar, keyCode);
    }
}
