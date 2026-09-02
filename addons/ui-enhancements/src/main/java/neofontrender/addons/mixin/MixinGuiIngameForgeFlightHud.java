package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiNewChat;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import neofontrender.addons.flight.FlightHudOverlayController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Suppresses only Minecraft's selected HUD draws while preserving Forge's overlay lifecycle. */
@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiIngameForgeFlightHud {
    @Shadow private RenderGameOverlayEvent eventParent;

    @Redirect(method = "renderHotbar", remap = true,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z"),
            require = 1)
    private boolean nfrUi$suppressOnlyVanillaHotbar(GuiIngameForge gui,
            RenderGameOverlayEvent.ElementType type) {
        return nfrUi$shouldSkipVanillaElement(type);
    }

    @Redirect(method = {
            "renderArmor", "renderHealth", "renderFood", "renderAir",
            "renderHealthMount", "renderJumpBar", "renderExperience", "renderPotionIcons",
            "renderSubtitles", "renderPlayerList"
    }, remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/client/GuiIngameForge;pre(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$ElementType;)Z"),
            require = 10)
    private boolean nfrUi$suppressOnlyVanillaForgeElement(GuiIngameForge gui,
            RenderGameOverlayEvent.ElementType type) {
        return nfrUi$shouldSkipVanillaElement(type);
    }

    @Unique
    private boolean nfrUi$shouldSkipVanillaElement(RenderGameOverlayEvent.ElementType type) {
        boolean canceled = MinecraftForge.EVENT_BUS.post(
                new RenderGameOverlayEvent.Pre(eventParent, type));
        if (canceled || !FlightHudOverlayController.shouldSuppressVanilla(type)) {
            return canceled;
        }
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(eventParent, type));
        return true;
    }

    @Redirect(method = "renderChat", remap = false,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/client/gui/GuiNewChat;drawChat(I)V"),
            require = 1)
    private void nfrUi$suppressOnlyVanillaChat(GuiNewChat chat, int updateCounter) {
        if (!FlightHudOverlayController.shouldSuppressVanilla(
                RenderGameOverlayEvent.ElementType.CHAT)) {
            chat.drawChat(updateCounter);
        }
    }

    @Redirect(method = "renderHUDText", remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z"),
            require = 1)
    private boolean nfrUi$postTextWithoutVanillaEntries(EventBus bus, Event event) {
        if (event instanceof RenderGameOverlayEvent.Text
                && FlightHudOverlayController.shouldSuppressVanilla(
                        RenderGameOverlayEvent.ElementType.TEXT)) {
            RenderGameOverlayEvent.Text text = (RenderGameOverlayEvent.Text) event;
            text.getLeft().clear();
            text.getRight().clear();
        }
        return bus.post(event);
    }
}
