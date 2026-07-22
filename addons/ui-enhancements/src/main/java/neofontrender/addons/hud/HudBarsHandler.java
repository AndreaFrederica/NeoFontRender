package neofontrender.addons.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.hud.api.HudBarElement;
import neofontrender.addons.hud.api.HudBarProvider;
import neofontrender.addons.hud.api.HudBarRegistry;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;
import neofontrender.addons.ui.NfrUiEnhancements;

final class HudBarsHandler {
    private final Arc3DHudBarRenderer renderer = new Arc3DHudBarRenderer();
    private boolean loggedClassicBar;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void render(RenderGameOverlayEvent.Pre event) {
        if (!HudBarsConfig.enabled || event.isCanceled()) return;
        if (HudBarsConfig.yieldToClassicBar && Loader.isModLoaded("classicbar")) {
            if (!loggedClassicBar) {
                loggedClassicBar = true;
                NfrUiEnhancements.LOGGER.info("Classic Bar detected; Arc3D status bars will yield to it");
            }
            return;
        }
        HudBarElement element = element(event.getType());
        if (element == null) return;
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (!(view instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) view;
        if (player.capabilities.isCreativeMode || player.isSpectator()) return;

        boolean replace = false;
        for (HudBarProvider provider : HudBarRegistry.snapshot(element)) {
            if (!provider.shouldRender(player)) continue;
            HudBarValue value;
            try {
                value = provider.sample(player, event.getPartialTicks());
            } catch (Throwable throwable) {
                NfrUiEnhancements.LOGGER.warn("HUD bar provider '{}' failed", provider.id(), throwable);
                continue;
            }
            if (value == null || value.maximum <= 0.0F) continue;
            HudBarSide side = provider.side();
            int offset = side == HudBarSide.RIGHT ? GuiIngameForge.right_height : GuiIngameForge.left_height;
            int x = side == HudBarSide.RIGHT
                    ? event.getResolution().getScaledWidth() / 2 + 10
                    : event.getResolution().getScaledWidth() / 2 - 91;
            int y = event.getResolution().getScaledHeight() - offset;
            renderer.draw(provider.id(), value, side, x, y);
            int reserve = HudBarsConfig.height + HudBarsConfig.gap;
            if (side == HudBarSide.RIGHT) GuiIngameForge.right_height += reserve;
            else GuiIngameForge.left_height += reserve;
            replace |= provider.replacesVanilla();
        }
        if (replace) event.setCanceled(true);
    }

    private static HudBarElement element(RenderGameOverlayEvent.ElementType type) {
        switch (type) {
            case HEALTH: return HudBarElement.HEALTH;
            case ARMOR: return HudBarElement.ARMOR;
            case FOOD: return HudBarElement.FOOD;
            case AIR: return HudBarElement.AIR;
            case HEALTHMOUNT: return HudBarElement.MOUNT_HEALTH;
            default: return null;
        }
    }
}
