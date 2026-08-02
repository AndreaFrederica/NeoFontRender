package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.Collections;

/** Shared link identity, double-click state and avatar tooltip for both chat renderers. */
public final class ChatPlayerLinks {
    static final String MARKER = "nfr-ui-player:";
    private static final long DOUBLE_CLICK_MS = 350L;

    private static String lastPlayer = "";
    private static long lastClickAt;
    private static String hoveredAvatar = "";
    private static int hoveredX;
    private static int hoveredY;

    private ChatPlayerLinks() {}

    public static String playerFrom(ITextComponent component) {
        if (component == null) return null;
        ClickEvent click = component.getStyle().getClickEvent();
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
        ScaledResolution resolution = new ScaledResolution(minecraft);
        String pattern = AddonI18n.tr("neofontrender_ui_enhancements.chat.player.tooltip");
        GuiUtils.drawHoveringText(Collections.singletonList(pattern.replace("%s", hoveredAvatar)),
                hoveredX, hoveredY, resolution.getScaledWidth(), resolution.getScaledHeight(),
                -1, minecraft.fontRenderer);
        hoveredAvatar = "";
    }
}
