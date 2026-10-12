package neofontrender.addons.notifications;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.loading.ModernLoadingPromptButton;
import neofontrender.addons.loading.ModernLoadingPromptRenderer;
import neofontrender.client.NeofontrenderCustomMainMenu;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * Input-blocking overlay drawn after the installed title-screen implementation.
 *
 * <p>1.7.10 Forge only publishes {@code InitGui/DrawScreen/ActionPerformed} screen events, so the
 * overlay cannot swallow mouse and keyboard input the way the newer platform's
 * {@code MouseInputEvent}/{@code KeyboardInputEvent} cancellations do. The title screen is instead
 * swapped for a wrapper that draws the real screen and owns all input until the player answers;
 * afterwards the real screen is handed back to {@code Minecraft} so every other title-screen
 * consumer keeps observing the vanilla (or Custom Main Menu) screen identity.</p>
 */
public final class CompatibilityNotificationPrompt {
    private GuiScreen dismissedScreen;

    public static void register() {
        CompatibilityNotificationRegistry.load();
        MinecraftForge.EVENT_BUS.register(new CompatibilityNotificationPrompt());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void opened(GuiOpenEvent event) {
        GuiScreen next = event.gui;
        if (next == null || next instanceof PromptScreen) return;
        if (!isTitleScreen(next)) {
            dismissedScreen = null;
            return;
        }
        if (next == dismissedScreen) return;
        List<CompatibilityNotification> notices = CompatibilityNotificationRegistry.pending();
        if (notices.isEmpty()) return;
        event.gui = new PromptScreen(next, notices);
    }

    private static boolean isTitleScreen(GuiScreen screen) {
        return screen instanceof GuiMainMenu || NeofontrenderCustomMainMenu.ownsScreen(screen);
    }

    /** Title-screen stand-in that presents the pending notices before any input reaches the host. */
    private final class PromptScreen extends GuiScreen {
        private final GuiScreen delegate;
        private final List<CompatibilityNotification> notices;
        private final ModernLoadingPromptButton exitButton;
        private final ModernLoadingPromptButton continueButton;
        private boolean handingBack;
        private int scrollLine;

        private PromptScreen(GuiScreen delegate, List<CompatibilityNotification> notices) {
            this.delegate = delegate;
            this.notices = notices;
            this.exitButton = new ModernLoadingPromptButton(0, 0, 0, 0,
                    I18n.format("neofontrender_ui_enhancements.notification.exit"), false);
            this.continueButton = new ModernLoadingPromptButton(1, 0, 0, 0,
                    I18n.format("neofontrender_ui_enhancements.notification.continue"), true);
        }

        @Override
        public void initGui() {
            // The replaced screen never reached setWorldAndResolution, so build it exactly like
            // vanilla would have before drawing it or forwarding ticks to it.
            delegate.setWorldAndResolution(mc, width, height);
            positionButtons();
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            delegate.drawScreen(mouseX, mouseY, partialTicks);
            positionButtons();
            Gui.drawRect(0, 0, width, height, 0xA0000000);
            ModernLoadingPromptRenderer.drawPanel(mc.fontRenderer,
                    I18n.format("neofontrender_ui_enhancements.notification.title"), body(),
                    width, height, scrollLine);
            exitButton.drawButton(mc, mouseX, mouseY);
            continueButton.drawButton(mc, mouseX, mouseY);
        }

        @Override
        public void updateScreen() {
            delegate.updateScreen();
        }

        @Override
        public void onGuiClosed() {
            if (handingBack) return;
            delegate.onGuiClosed();
        }

        @Override
        public void handleMouseInput() {
            int wheel = Mouse.getEventDWheel();
            if (wheel != 0) {
                ModernLoadingPromptRenderer.Layout layout = layout();
                scrollLine = Math.max(0,
                        Math.min(layout.maxScroll(), scrollLine + (wheel < 0 ? 3 : -3)));
                return;
            }
            if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
            int mouseX = Mouse.getEventX() * width / Math.max(1, mc.displayWidth);
            int mouseY = height - Mouse.getEventY() * height / Math.max(1, mc.displayHeight) - 1;
            if (exitButton.mousePressed(mc, mouseX, mouseY)) {
                exitButton.func_146113_a(mc.getSoundHandler());
                mc.shutdown();
            } else if (continueButton.mousePressed(mc, mouseX, mouseY)) {
                continueButton.func_146113_a(mc.getSoundHandler());
                dismiss();
            }
        }

        // Input stays with the prompt: the mouse is answered in handleMouseInput, and neither the
        // host screen nor a stray vault key may navigate away while the notices are unanswered.
        @Override
        protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {}

        @Override
        protected void keyTyped(char typedChar, int keyCode) {}

        private void dismiss() {
            CompatibilityNotificationRegistry.markShown(notices);
            dismissedScreen = delegate;
            handingBack = true;
            mc.displayGuiScreen(delegate);
            handingBack = false;
        }

        private ModernLoadingPromptRenderer.Layout layout() {
            return ModernLoadingPromptRenderer.layout(mc.fontRenderer, body(), width, height);
        }

        private void positionButtons() {
            ModernLoadingPromptRenderer.Layout layout = layout();
            int buttonWidth = Math.min(150, Math.max(90, (layout.width - 38) / 2));
            int gap = 10;
            int left = width / 2 - buttonWidth - gap / 2;
            exitButton.width = buttonWidth;
            exitButton.xPosition = left;
            exitButton.yPosition = layout.buttonY();
            continueButton.width = buttonWidth;
            continueButton.xPosition = left + buttonWidth + gap;
            continueButton.yPosition = layout.buttonY();
        }

        private String body() {
            StringBuilder result = new StringBuilder();
            for (CompatibilityNotification notice : notices) {
                if (result.length() > 0) result.append("\n\n");
                result.append(translate(notice.resolvedTitleKey(), notice.modid)).append('\n')
                        .append(translate(notice.resolvedMessageKey(), notice.modid));
            }
            return result.toString();
        }
    }

    private static String translate(String key, String fallback) {
        String value = I18n.format(key);
        return key.equals(value) ? fallback : value;
    }
}
