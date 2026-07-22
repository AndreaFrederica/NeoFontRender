package neofontrender.addons.hud;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import neofontrender.addons.hud.api.HudBarElement;
import neofontrender.addons.hud.api.HudBarProvider;
import neofontrender.addons.hud.api.HudBarRegistration;
import neofontrender.addons.hud.api.HudBarRegistry;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.HashSet;
import java.util.Set;

/** Renders registered providers in Forge's native HUD layout and cancels only successful replacements. */
final class HudBarsHandler {
    private final Arc3DHudBarRenderer renderer = new Arc3DHudBarRenderer();
    private final Set<String> failedProviders = new HashSet<String>();
    private boolean loggedClassicBar;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void render(RenderGameOverlayEvent.Pre event) {
        if (!HudBarsConfig.enabled || event.isCanceled()) return;
        if (HudBarsConfig.yieldToClassicBar && Loader.isModLoaded("classicbar")) {
            if (!loggedClassicBar) {
                loggedClassicBar = true;
                NfrUiEnhancements.LOGGER.info("Classic Bar detected; NFR HUD bars will yield to it");
            }
            return;
        }
        HudBarElement element = element(event.type);
        if (element == null) return;
        Entity view = Minecraft.getMinecraft().renderViewEntity;
        if (!(view instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) view;
        if (player.capabilities.isCreativeMode) return;

        boolean replaceVanilla = false;
        for (HudBarRegistration registration : HudBarRegistry.snapshot(element)) {
            HudBarProvider provider = registration.provider();
            try {
                if (!provider.shouldRender(player)) continue;
                HudBarValue value = provider.sample(player, event.partialTicks);
                if (value == null) throw new IllegalStateException("provider returned a null HUD value");
                draw(registration, value, event);
                replaceVanilla |= registration.replacesVanilla();
                failedProviders.remove(registration.id());
            } catch (RuntimeException | LinkageError exception) {
                if (failedProviders.add(registration.id())) {
                    NfrUiEnhancements.LOGGER.error(
                            "HUD bar provider '{}' failed; vanilla rendering remains active for this element",
                            registration.id(), exception);
                }
            }
        }
        if (replaceVanilla) event.setCanceled(true);
    }

    private void draw(HudBarRegistration registration, HudBarValue value, RenderGameOverlayEvent.Pre event) {
        HudBarSide side = registration.side();
        int offset = side == HudBarSide.RIGHT ? GuiIngameForge.right_height : GuiIngameForge.left_height;
        int center = event.resolution.getScaledWidth() / 2;
        int x = side == HudBarSide.RIGHT ? center + 10 : center - 10 - HudBarsConfig.width;
        int y = event.resolution.getScaledHeight() - offset;
        renderer.draw(registration.id(), value, side, x, y);
        int reserve = HudBarsConfig.height + HudBarsConfig.gap;
        if (side == HudBarSide.RIGHT) GuiIngameForge.right_height += reserve;
        else GuiIngameForge.left_height += reserve;
    }

    private static HudBarElement element(RenderGameOverlayEvent.ElementType type) {
        switch (type) {
            case HEALTH:
                return HudBarElement.HEALTH;
            case ARMOR:
                return HudBarElement.ARMOR;
            case FOOD:
                return HudBarElement.FOOD;
            case AIR:
                return HudBarElement.AIR;
            case HEALTHMOUNT:
                return HudBarElement.MOUNT_HEALTH;
            default:
                return null;
        }
    }
}
