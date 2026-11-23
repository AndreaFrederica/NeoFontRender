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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** CMM uses a fake init event list. Work on the real screen after all init handlers finish. */
public final class CustomMainMenuButtonInjector {
    private static final Map<GuiButton, LastPlayedTarget> customContinueTargets = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiScreen screen = event.getGui();
        if (!NeofontrenderCustomMainMenu.ownsScreen(screen)) return;
        List<GuiButton> buttons = buttons(screen);
        if (buttons == null) return;
        LastPlayedTarget target = LastPlayedGameManager.INSTANCE.availableTarget();
        updateButton(buttons, MainMenuConfig.continueGame && !Minecraft.getMinecraft().isDemo(), target, () -> {
            if (target == null) return null;
            GuiButton anchor = anchor(buttons, screen);
            int x = anchor.x;
            int y = anchor.y - 24;
            int width = anchor.width;
            GuiButton styled = CustomMainMenuButtonSkin.create(screen, target, x, y, width,
                    cmmStyleSource(buttons, anchor));
            return styled != null ? styled : MainMenuContinueButton.create(screen, target, x, y, width);
        });
    }

    static void updateButton(List<GuiButton> buttons, boolean enabled, Supplier<GuiButton> factory) {
        updateButton(buttons, enabled, null, factory);
    }

    private static void updateButton(List<GuiButton> buttons, boolean enabled,
                                     LastPlayedTarget target, Supplier<GuiButton> factory) {
        if (!enabled) {
            buttons.removeIf(CustomMainMenuButtonInjector::isContinueButton);
            return;
        }
        // initGui clears this list on resize/reopen. Check the real list, not a remembered GUI.
        if (findContinue(buttons) != null) return;
        GuiButton button = factory.get();
        if (button != null) {
            buttons.add(button);
            if (CustomMainMenuButtonSkin.isCmmButton(button)) {
                // The object identity is stable for CMM's renderer. Keep the exact target used
                // to build the label instead of looking it up again on a later frame.
                if (target != null) customContinueTargets.put(button, target);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        GuiScreen screen = event.getGui();
        boolean fancy = NeofontrenderFancyMenu.ownsScreen(screen);
        if (!fancy && !NeofontrenderCustomMainMenu.ownsScreen(screen)) return;
        GuiButton button = findContinue(buttons(screen));
        LastPlayedTarget target = continueTarget(button);
        if (button == null || target == null || !button.visible || !button.enabled
                || !MainMenuConfig.continueGame) return;
        if (fancy && NeofontrenderFancyMenu.isButtonHidden(screen, button)) return;
        int x = event.getMouseX(), y = event.getMouseY();
        if (x >= button.x && x < button.x + button.width && y >= button.y && y < button.y + button.height) {
            screen.drawHoveringText(MainMenuContinueButton.tooltip(target), x, y);
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!NeofontrenderCustomMainMenu.ownsScreen(event.getGui())) return;
        GuiButton button = event.getButton();
        LastPlayedTarget target = continueTarget(button);
        if (target == null) return;
        event.setCanceled(true);
        if (MainMenuConfig.continueGame && button.enabled
                && !LastPlayedGameManager.INSTANCE.resume(event.getGui(), target)) {
            button.enabled = false;
        }
    }

    private static List<GuiButton> buttons(GuiScreen screen) {
        return screen instanceof AccessorGuiScreenNavigation
                ? ((AccessorGuiScreenNavigation) screen).nfrUi$getNavigationButtons() : null;
    }

    private static GuiButton findContinue(List<GuiButton> buttons) {
        if (buttons != null) for (GuiButton button : buttons) if (isContinueButton(button)) return button;
        return null;
    }

    private static boolean isContinueButton(GuiButton button) {
        return MainMenuContinueButton.isContinueButton(button) || customContinueTargets.containsKey(button);
    }

    private static LastPlayedTarget continueTarget(GuiButton button) {
        if (button instanceof MainMenuContinueButton) return ((MainMenuContinueButton) button).target();
        return customContinueTargets.get(button);
    }

    private static GuiButton cmmStyleSource(List<GuiButton> buttons, GuiButton anchor) {
        if (CustomMainMenuButtonSkin.isCmmButton(anchor)) return anchor;
        if (buttons != null) for (GuiButton button : buttons) {
            if (CustomMainMenuButtonSkin.isCmmButton(button)) return button;
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
