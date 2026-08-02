package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ChatHeadResolver {
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    private ChatHeadResolver() {}

    public static UUID detect(ITextComponent message) {
        return EnhancedChatFeatures.playerHeads() ? detectSender(message) : null;
    }

    public static UUID detectSender(ITextComponent message) {
        if (message == null) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        NetHandlerPlayClient connection = minecraft.getConnection();
        if (connection == null) return null;

        UUID suggested = suggestedPlayer(message, connection);
        if (suggested != null) return suggested;

        Map<String, UUID> names = new LinkedHashMap<>();
        for (NetworkPlayerInfo player : connection.getPlayerInfoMap()) {
            String profileName = clean(player.getGameProfile().getName());
            if (!profileName.isEmpty()) names.put(profileName, player.getGameProfile().getId());
            if (player.getDisplayName() != null) {
                String displayName = clean(player.getDisplayName().getUnformattedText());
                if (!displayName.isEmpty()) names.putIfAbsent(displayName, player.getGameProfile().getId());
            }
        }
        return ChatPlayerNameMatcher.find(clean(message.getUnformattedText()), names);
    }

    public static void beginVanillaLine(ITextComponent message) {
        ChatMessageMetadata metadata = ChatMessageMetadataRegistry.get(message);
        UUID sender = metadata != null && metadata.playerId != null ? metadata.playerId : detect(message);
        PENDING.set(new Pending(sender, metadata));
    }

    public static Capture captureVanillaLine() {
        Pending pending = PENDING.get();
        if (pending == null) return Capture.EMPTY;
        Capture capture = new Capture(pending.senderId, pending.firstFragment, pending.metadata);
        pending.firstFragment = false;
        return capture;
    }

    public static void endVanillaLine() {
        PENDING.remove();
    }

    private static UUID suggestedPlayer(ITextComponent message, NetHandlerPlayClient connection) {
        for (ITextComponent part : message) {
            ClickEvent event = part.getStyle().getClickEvent();
            if (event == null || event.getAction() != ClickEvent.Action.SUGGEST_COMMAND) continue;
            String value = event.getValue();
            if (value == null) continue;
            String[] tokens = value.trim().split("\\s+");
            if (tokens.length < 2 || !(tokens[0].equalsIgnoreCase("/tell")
                    || tokens[0].equalsIgnoreCase("/msg") || tokens[0].equalsIgnoreCase("/w"))) continue;
            NetworkPlayerInfo player = connection.getPlayerInfo(tokens[1]);
            if (player != null) return player.getGameProfile().getId();
        }
        return null;
    }

    private static String clean(String value) {
        String clean = TextFormatting.getTextWithoutFormattingCodes(value);
        return clean == null ? "" : clean;
    }

    private static final class Pending {
        private final UUID senderId;
        private final ChatMessageMetadata metadata;
        private boolean firstFragment = true;

        private Pending(UUID senderId, ChatMessageMetadata metadata) {
            this.senderId = senderId;
            this.metadata = metadata;
        }
    }

    public static final class Capture {
        private static final Capture EMPTY = new Capture(null, false, null);
        public final UUID senderId;
        public final boolean firstFragment;
        public final ChatMessageMetadata metadata;

        private Capture(UUID senderId, boolean firstFragment, ChatMessageMetadata metadata) {
            this.senderId = senderId;
            this.firstFragment = firstFragment;
            this.metadata = metadata;
        }
    }
}
