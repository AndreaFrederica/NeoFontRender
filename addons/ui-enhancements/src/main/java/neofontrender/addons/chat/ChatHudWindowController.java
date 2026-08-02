package neofontrender.addons.chat;

import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import mnm.mods.tabbychat.gui.ChatBox;
import mnm.mods.util.ILocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.hud.compositor.HudSurface;
import neofontrender.addons.hud.compositor.HudWindowCompositor;

import java.awt.Rectangle;

/** Hosts TabbyChat in the generic HUD compositor and manages hold-to-interact cursor capture. */
public final class ChatHudWindowController {
    public static final ChatHudWindowController INSTANCE = new ChatHudWindowController();
    private static final String SURFACE_ID = "neofontrender_ui_enhancements:chat";

    private final Minecraft mc = Minecraft.getMinecraft();
    private final TabbySurface surface = new TabbySurface();
    private GuiNewChatTC queuedGui;
    private int updateCounter;
    private boolean hudInteractive;

    private ChatHudWindowController() {
        HudWindowCompositor.INSTANCE.register(surface);
    }

    public void queue(GuiNewChatTC gui, int counter) {
        this.queuedGui = gui;
        this.updateCounter = counter;
    }

    public static boolean isChatExpanded() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.currentScreen instanceof GuiChat
                || EnhancedChatConfigAccess.persistentChatHudEnabled()
                && mc.currentScreen == null && mc.player != null && mc.world != null;
    }

    public static boolean isHudInteractive() {
        return INSTANCE.hudInteractive;
    }

    public static void toggleDetached() {
        Minecraft mc = Minecraft.getMinecraft();
        boolean detached = !EnhancedChatConfigAccess.persistentChatHudEnabled();
        EnhancedChatConfigAccess.setPersistentChatHudEnabled(detached);
        if (detached && EnhancedChatConfigAccess.closeChatOnDetach()
                && mc.currentScreen instanceof GuiChat) {
            mc.addScheduledTask(() -> mc.displayGuiScreen(null));
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL && queuedGui != null) {
            HudWindowCompositor.INSTANCE.render(event.getPartialTicks());
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean interact = EnhancedChatConfigAccess.persistentChatHudEnabled()
                && mc.currentScreen == null && mc.player != null && ChatKeyBindings.hudInteractionDown();
        if (interact == hudInteractive) return;
        hudInteractive = interact;
        if (hudInteractive) {
            // Also clears Minecraft's in-game focus flag so mouse deltas cannot rotate the camera.
            mc.setIngameNotInFocus();
        } else if (mc.currentScreen == null && mc.player != null) {
            mc.setIngameFocus();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouseInput(MouseEvent event) {
        if (!hudInteractive || queuedGui == null) return;
        // While the compositor owns the pointer, no click, wheel or movement event may leak into
        // Minecraft. Events that hit a surface are forwarded after ownership is established.
        event.setCanceled(true);
        ScaledResolution resolution = new ScaledResolution(mc);
        int x = event.getX() * resolution.getScaledWidth() / mc.displayWidth;
        int y = resolution.getScaledHeight()
                - event.getY() * resolution.getScaledHeight() / mc.displayHeight - 1;
        if (!HudWindowCompositor.INSTANCE.mouseInput(
                x, y, event.getButton(), event.isButtonstate())) return;

        if (event.getButton() == 0 && event.isButtonstate() && inputBounds().contains(x, y)) {
            mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiChat()));
        }
    }

    private Rectangle inputBounds() {
        ChatBox chatBox = TabbyChat.getInstance().getChat().getChatBox();
        ILocation location = chatBox.getChatInput().getActualLocation();
        return new Rectangle(location.getXPos(), location.getYPos(),
                location.getWidth(), location.getHeight());
    }

    private final class TabbySurface implements HudSurface {
        @Override public String id() { return SURFACE_ID; }

        @Override public Rectangle bounds() {
            if (queuedGui == null || TabbyChat.getInstance().getChat() == null) return new Rectangle();
            ILocation location = TabbyChat.getInstance().getChat().getChatBox().getActualLocation();
            return new Rectangle(location.getXPos(), location.getYPos(),
                    location.getWidth(), location.getHeight());
        }

        @Override public boolean visible() {
            return queuedGui != null && EnhancedChatConfigAccess.tabbedChatEnabled()
                    && mc.player != null && mc.world != null;
        }

        @Override public void render(float partialTicks) {
            queuedGui.nfrUi$drawChatSurface(updateCounter);
        }

        @Override public boolean acceptsPointer() { return hudInteractive; }

        @Override public void mouseInput() {
            TabbyChat.getInstance().getChat().getChatBox().handleMouseInput();
        }
    }
}
