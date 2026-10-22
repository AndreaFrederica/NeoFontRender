package neofontrender.addons.notifications;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.loading.ModernLoadingPromptButton;
import neofontrender.addons.loading.ModernLoadingPromptRenderer;
import neofontrender.client.NeofontrenderCustomMainMenu;
import org.lwjgl.input.Mouse;

import java.util.Collections;
import java.util.List;

/** Input-blocking overlay drawn after the installed title-screen implementation. */
public final class CompatibilityNotificationPrompt {
    private GuiScreen activeScreen;
    private GuiScreen dismissedScreen;
    private List<CompatibilityNotification> notices = Collections.emptyList();
    private ModernLoadingPromptButton exitButton;
    private ModernLoadingPromptButton continueButton;
    private int scrollLine;

    public static void register() {
        CompatibilityNotificationRegistry.load();
        MinecraftForge.EVENT_BUS.register(new CompatibilityNotificationPrompt());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        if (!isTitleScreen(screen) || screen == dismissedScreen) return;
        if (activeScreen != screen) open(screen);
        if (notices.isEmpty()) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        String body = body();
        positionButtons(screen, body);
        Gui.drawRect(0, 0, screen.width, screen.height, 0xA0000000);
        ModernLoadingPromptRenderer.drawPanel(minecraft.fontRenderer,
                I18n.format("neofontrender_ui_enhancements.notification.title"), body,
                screen.width, screen.height, scrollLine);
        exitButton.drawButton(minecraft, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
        continueButton.drawButton(minecraft, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event.getGui() != activeScreen || notices.isEmpty()) return;
        event.setCanceled(true);
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            ModernLoadingPromptRenderer.Layout layout = ModernLoadingPromptRenderer.layout(
                    Minecraft.getMinecraft().fontRenderer, body(), activeScreen.width, activeScreen.height);
            scrollLine = Math.max(0, Math.min(layout.maxScroll(), scrollLine + (wheel < 0 ? 3 : -3)));
            return;
        }
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        int mouseX = mouseX();
        int mouseY = mouseY();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (exitButton != null && exitButton.mousePressed(minecraft, mouseX, mouseY)) {
            exitButton.playPressSound(minecraft.getSoundHandler());
            minecraft.shutdown();
        } else if (continueButton != null && continueButton.mousePressed(minecraft, mouseX, mouseY)) {
            continueButton.playPressSound(minecraft.getSoundHandler());
            CompatibilityNotificationRegistry.markShown(notices);
            dismissedScreen = activeScreen;
            close();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void keyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event.getGui() == activeScreen && !notices.isEmpty()) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void opened(GuiOpenEvent event) {
        GuiScreen next = event.getGui();
        if (next != activeScreen) close();
        if (!isTitleScreen(next)) dismissedScreen = null;
    }

    private void open(GuiScreen screen) {
        activeScreen = screen;
        notices = CompatibilityNotificationRegistry.pending();
        scrollLine = 0;
        exitButton = null;
        continueButton = null;
    }

    private void close() {
        activeScreen = null;
        notices = Collections.emptyList();
        exitButton = null;
        continueButton = null;
        scrollLine = 0;
    }

    private void positionButtons(GuiScreen screen, String body) {
        ModernLoadingPromptRenderer.Layout layout = ModernLoadingPromptRenderer.layout(
                Minecraft.getMinecraft().fontRenderer, body, screen.width, screen.height);
        int buttonWidth = Math.min(150, Math.max(90, (layout.width - 38) / 2));
        int gap = 10;
        int left = screen.width / 2 - buttonWidth - gap / 2;
        if (exitButton == null) {
            exitButton = new ModernLoadingPromptButton(0, left, layout.buttonY(), buttonWidth,
                    I18n.format("neofontrender_ui_enhancements.notification.exit"), false);
            continueButton = new ModernLoadingPromptButton(1, left + buttonWidth + gap,
                    layout.buttonY(), buttonWidth,
                    I18n.format("neofontrender_ui_enhancements.notification.continue"), true);
        } else {
            exitButton.x = left;
            exitButton.y = layout.buttonY();
            exitButton.width = buttonWidth;
            continueButton.x = left + buttonWidth + gap;
            continueButton.y = layout.buttonY();
            continueButton.width = buttonWidth;
        }
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

    private static String translate(String key, String fallback) {
        String value = I18n.format(key);
        return key.equals(value) ? fallback : value;
    }

    private static boolean isTitleScreen(GuiScreen screen) {
        return screen instanceof GuiMainMenu || NeofontrenderCustomMainMenu.ownsScreen(screen);
    }

    private static int mouseX() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int mouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }
}
