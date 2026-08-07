package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import net.minecraft.event.ClickEvent;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.Collections;
import java.util.List;

/** Shared link identity, double-click state and avatar tooltip for both chat renderers. */
public final class ChatPlayerLinks {
    static final String MARKER = "nfr-ui-player:";
    private static final long DOUBLE_CLICK_MS = 350L;

    private static String lastPlayer = "";
    private static long lastClickAt;
    private static String hoveredAvatar = "";
    private static int hoveredX;
    private static int hoveredY;
    private static final TooltipCanvas TOOLTIP = new TooltipCanvas();

    private ChatPlayerLinks() {}

    public static String playerFrom(IChatComponent component) {
        if (component == null) return null;
        ClickEvent click = component.getChatStyle().getChatClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.SUGGEST_COMMAND
                || !click.getValue().startsWith(MARKER)) return null;
        String player = click.getValue().substring(MARKER.length());
        return player.isEmpty() ? null : player;
    }

    public static boolean activate(String player) {
        if (player == null || player.isEmpty()) return false;
        long now = System.currentTimeMillis();
        boolean doubleClick = player.equalsIgnoreCase(lastPlayer) && now - lastClickAt <= DOUBLE_CLICK_MS;
        lastPlayer = player;
        lastClickAt = now;
        if (doubleClick) {
            lastPlayer = "";
            ChatPlayerActions.startPrivateMessage(player);
        }
        return doubleClick;
    }

    public static void hoverAvatar(String player, int mouseX, int mouseY) {
        hoveredAvatar = player == null ? "" : player;
        hoveredX = mouseX;
        hoveredY = mouseY;
    }

    public static void drawAvatarTooltip() {
        if (hoveredAvatar.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        String pattern = AddonI18n.tr("neofontrender_ui_enhancements.chat.player.tooltip");
        TOOLTIP.draw(Collections.singletonList(pattern.replace("%s", hoveredAvatar)),
                hoveredX, hoveredY, resolution, minecraft.fontRenderer);
        hoveredAvatar = "";
    }

    private static final class TooltipCanvas extends GuiScreen {
        private void draw(List<String> lines, int x, int y, ScaledResolution resolution,
                          FontRenderer font) {
            this.mc = Minecraft.getMinecraft();
            this.fontRendererObj = font;
            this.width = resolution.getScaledWidth();
            this.height = resolution.getScaledHeight();
            drawHoveringText(lines, x, y, font);
        }
    }
}
