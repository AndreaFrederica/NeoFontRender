package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.core.GuiNewChatTC;
import neofontrender.addons.vendor.tabbychat.gui.ChatBox;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
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
                && mc.currentScreen == null && mc.thePlayer != null && mc.theWorld != null;
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
            mc.func_152344_a(() -> mc.displayGuiScreen(null));
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL && queuedGui != null) {
            if (hudInteractive && mc.currentScreen == null) {
                ChatInlineImageInteraction.draw(0, 0);
            }
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean interact = EnhancedChatConfigAccess.persistentChatHudEnabled()
                && mc.currentScreen == null && mc.thePlayer != null && ChatKeyBindings.hudInteractionDown();
        if (interact == hudInteractive) return;
        hudInteractive = interact;
        if (hudInteractive) {
            // Also clears Minecraft's in-game focus flag so mouse deltas cannot rotate the camera.
            mc.setIngameNotInFocus();
        } else if (mc.currentScreen == null && mc.thePlayer != null) {
            mc.setIngameFocus();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouseInput(MouseEvent event) {
        if (!hudInteractive || queuedGui == null) return;
        // While the compositor owns the pointer, no click, wheel or movement event may leak into
        // Minecraft. Events that hit a surface are forwarded after ownership is established.
        event.setCanceled(true);
        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int x = event.x * resolution.getScaledWidth() / mc.displayWidth;
        int y = resolution.getScaledHeight()
                - event.y * resolution.getScaledHeight() / mc.displayHeight - 1;
        if (!HudWindowCompositor.INSTANCE.mouseInput(
                x, y, event.button, event.buttonstate)) return;

        if (event.button == 0 && event.buttonstate && inputBounds().contains(x, y)) {
            mc.func_152344_a(() -> mc.displayGuiScreen(new GuiChat()));
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
                    && mc.thePlayer != null && mc.theWorld != null;
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
