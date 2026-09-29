package neofontrender.addons.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorGuiScreenNavigation;
import neofontrender.client.NeofontrenderCustomMainMenu;
import neofontrender.client.NeofontrenderFancyMenu;

import java.util.List;
import java.util.function.Supplier;

/** CMM uses a fake init event list. Work on the real screen after all init handlers finish. */
public final class CustomMainMenuButtonInjector {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiScreen screen = event.getGui();
        if (!NeofontrenderCustomMainMenu.ownsScreen(screen)) return;
        List<GuiButton> buttons = buttons(screen);
        if (buttons == null) return;
        updateButton(buttons, MainMenuConfig.continueGame && !Minecraft.getMinecraft().isDemo(), () -> {
            LastPlayedTarget target = LastPlayedGameManager.INSTANCE.availableTarget();
            if (target == null) return null;
            GuiButton anchor = anchor(buttons, screen);
            return MainMenuContinueButton.create(screen, target, anchor.x, anchor.y - 24, anchor.width);
        });
    }

    static void updateButton(List<GuiButton> buttons, boolean enabled, Supplier<GuiButton> factory) {
        if (!enabled) {
            buttons.removeIf(MainMenuContinueButton::isContinueButton);
            return;
        }
        // initGui clears this list on resize/reopen. Check the real list, not a remembered GUI.
        if (findContinue(buttons) != null) return;
        GuiButton button = factory.get();
        if (button != null) buttons.add(button);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        boolean fancy = NeofontrenderFancyMenu.ownsScreen(screen);
        if (!fancy && !NeofontrenderCustomMainMenu.ownsScreen(screen)) return;
        MainMenuContinueButton button = findContinue(buttons(screen));
        if (button == null || !button.visible || !button.enabled || !MainMenuConfig.continueGame) return;
        if (fancy && NeofontrenderFancyMenu.isButtonHidden(screen, button)) return;
        int x = event.getMouseX(), y = event.getMouseY();
        if (x >= button.x && x < button.x + button.width && y >= button.y && y < button.y + button.height) {
            screen.drawHoveringText(MainMenuContinueButton.tooltip(button.target()), x, y);
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!NeofontrenderCustomMainMenu.ownsScreen(event.getGui())
                || !(event.getButton() instanceof MainMenuContinueButton)) return;
        MainMenuContinueButton button = (MainMenuContinueButton) event.getButton();
        event.setCanceled(true);
        if (MainMenuConfig.continueGame && button.enabled
                && !LastPlayedGameManager.INSTANCE.resume(event.getGui(), button.target())) {
            button.enabled = false;
        }
    }

    private static List<GuiButton> buttons(GuiScreen screen) {
        return screen instanceof AccessorGuiScreenNavigation
                ? ((AccessorGuiScreenNavigation) screen).nfrUi$getNavigationButtons() : null;
    }

    private static MainMenuContinueButton findContinue(List<GuiButton> buttons) {
        if (buttons != null) for (GuiButton button : buttons) {
            if (button instanceof MainMenuContinueButton) return (MainMenuContinueButton) button;
        }
        return null;
    }

    private static GuiButton anchor(List<GuiButton> buttons, GuiScreen screen) {
        // Wrapped singleplayer has id 1 in CMM. Avoid hidden/zero-sized layout elements.
        for (GuiButton button : buttons) {
            if (button.id == 1 && button.visible && button.width > 0) return button;
        }
        for (GuiButton button : buttons) {
            if (button.visible && button.width > 0 && button.getClass().getName()
                    .equals("lumien.custommainmenu.gui.GuiCustomButton")) return button;
        }
        return new GuiButton(1, screen.width / 2 - 100, screen.height / 4 + 48, 200, 20, "");
    }
}
