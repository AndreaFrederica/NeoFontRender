package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves the sending player of a chat component to an online player name.
 * Minecraft 1.7.10 has no UUID-keyed NetworkPlayerInfo, so names from the tab-list
 * and the client world act as the identity source for skin lookups.
 */
public final class ChatHeadResolver {
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    private ChatHeadResolver() {}

    public static String detect(IChatComponent message) {
        if (!EnhancedChatFeatures.playerHeads() || message == null) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        NetHandlerPlayClient connection = minecraft.getNetHandler();
        if (connection == null) return null;

        Map<String, String> names = playerNames(minecraft, connection);
        String suggested = suggestedPlayer(message, names);
        if (suggested != null) return suggested;
        return ChatPlayerNameMatcher.find(clean(message.getUnformattedText()), names);
    }

    public static void beginVanillaLine(IChatComponent message) {
        PENDING.set(new Pending(detect(message)));
    }

    public static Capture captureVanillaLine() {
        Pending pending = PENDING.get();
        if (pending == null) return Capture.EMPTY;
        Capture capture = new Capture(pending.senderName, pending.firstFragment);
        pending.firstFragment = false;
        return capture;
    }

    public static void endVanillaLine() {
        PENDING.remove();
    }

    /** Maps every known display alias to the exact account name used for skin lookups. */
    private static Map<String, String> playerNames(Minecraft minecraft, NetHandlerPlayClient connection) {
        Map<String, String> names = new LinkedHashMap<>();
        for (GuiPlayerInfo info : connection.playerInfoList) {
            String name = clean(info.name);
            if (!name.isEmpty()) names.put(name, name);
        }
        if (minecraft.theWorld != null) {
            for (Object entry : minecraft.theWorld.playerEntities) {
                EntityPlayer player = (EntityPlayer) entry;
                String accountName = clean(player.getCommandSenderName());
                if (accountName.isEmpty()) continue;
                if (!names.containsKey(accountName)) names.put(accountName, accountName);
                String displayName = clean(player.getDisplayName());
                if (!displayName.isEmpty() && !names.containsKey(displayName)) {
                    names.put(displayName, accountName);
                }
            }
        }
        return names;
    }

    private static String suggestedPlayer(IChatComponent message, Map<String, String> names) {
        for (Iterator<?> parts = message.iterator(); parts.hasNext();) {
            IChatComponent part = (IChatComponent) parts.next();
            ClickEvent event = part.getChatStyle().getChatClickEvent();
            if (event == null || event.getAction() != ClickEvent.Action.SUGGEST_COMMAND) continue;
            String value = event.getValue();
            if (value == null) continue;
            String[] tokens = value.trim().split("\\s+");
            if (tokens.length < 2 || !(tokens[0].equalsIgnoreCase("/tell")
                    || tokens[0].equalsIgnoreCase("/msg") || tokens[0].equalsIgnoreCase("/w"))) continue;
            for (Map.Entry<String, String> entry : names.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(tokens[1])) return entry.getValue();
            }
        }
        return null;
    }

    private static String clean(String value) {
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value);
        return clean == null ? "" : clean;
    }

    private static final class Pending {
        private final String senderName;
        private boolean firstFragment = true;

        private Pending(String senderName) {
            this.senderName = senderName;
        }
    }

    public static final class Capture {
        private static final Capture EMPTY = new Capture(null, false);
        public final String senderName;
        public final boolean firstFragment;

        private Capture(String senderName, boolean firstFragment) {
            this.senderName = senderName;
            this.firstFragment = firstFragment;
        }
    }
}
