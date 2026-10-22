package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Slot;
import net.minecraft.client.gui.inventory.GuiContainer;
import neofontrender.addons.mixin.AccessorGuiContainer;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Prevents a tooltip for the hidden vanilla hotbar from reappearing over RPG HUD Lite's HUD.
 * RPG HUD Lite cancels the HOTBAR overlay event, while tooltips use a separate Forge event and
 * would otherwise still be consumed and redrawn by UIE.
 */
final class RpgHudLiteTooltipCompat {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void suppressHiddenHotbarTooltip(RenderTooltipEvent.Pre event) {
        if (!Loader.isModLoaded("rpghudlite")) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || event.getStack() == null || event.getStack().isEmpty()) return;

        // When a real container slot is under the cursor, preserve its tooltip even if
        // the same stack also exists in the player's hotbar. RPGHud's own HUD slots do
        // not belong to GuiContainer, so they remain suppressible while a screen is open.
        if (minecraft.currentScreen instanceof GuiContainer) {
            Slot slot = ((AccessorGuiContainer) minecraft.currentScreen)
                    .nfrUi$getSlotAtPosition(event.getX(), event.getY());
            if (slot != null && ItemStack.areItemStacksEqual(event.getStack(), slot.getStack())) {
                return;
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack hotbarStack = player.inventory.getStackInSlot(slot);
            if (ItemStack.areItemStacksEqual(event.getStack(), hotbarStack)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
