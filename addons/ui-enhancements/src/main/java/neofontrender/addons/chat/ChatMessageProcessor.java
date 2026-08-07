package neofontrender.addons.chat;

import neofontrender.addons.vendor.tabbychat.ChatManager;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import neofontrender.addons.chat.network.SelfMessageCapability;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.Iterator;
import java.util.UUID;

public enum ChatMessageProcessor {
    INSTANCE;

    private static final String GROUP_INCOMING = "nfr.group.message.incoming";
    private static final String GROUP_OUTGOING = "nfr.group.message.outgoing";

    private static ChatOutgoingMessage recentOutgoing;
    private String lastMentionText = "";
    private long lastMentionSoundAt;

    static void recordOutgoing(String message) {
        if (!EnhancedChatConfig.enabled || message == null) return;
        recentOutgoing = ChatOutgoingMessage.parse(applyActivePrivateCommand(message),
                EnhancedChatConfig.privateMessageCommand, System.currentTimeMillis());
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        SelfMessageCapability.resetClient();
        Minecraft.getMinecraft().func_152344_a(SelfMessageCapability::probeServer);
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        SelfMessageCapability.resetClient();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void received(ClientChatReceivedEvent event) {
        if (!EnhancedChatConfig.enabled) return;
        IChatComponent component = event.message;
        UUID playerId = ChatHeadResolver.detectSender(component);
        String playerName = playerName(playerId);
        if (playerName.isEmpty()) {
            String detected = ChatHeadResolver.detectName(component);
            playerName = detected == null ? "" : detected;
        }
        String text = component.getUnformattedText();
        long now = System.currentTimeMillis();
        ChatOutgoingMessage outgoing = recentOutgoing;
        boolean outgoingMatch = outgoing != null && outgoing.matches(text, now);
        boolean groupMessage = EnhancedChatConfig.sourceClassification && isGroupMessage(component);
        String group = "";
        if (groupMessage) {
            String[] parts = groupParts(component);
            group = parts[0];
            playerName = parts[1];
            outgoingMatch = "1".equals(parts[2]);
            if (outgoingMatch) {
                playerId = localPlayerId();
                playerName = localPlayerName();
            }
        } else if (outgoingMatch) {
            playerId = localPlayerId();
            playerName = localPlayerName();
        }
        ChatSource source;
        if (groupMessage) {
            source = ChatSource.GROUP;
        } else if (EnhancedChatConfig.sourceClassification) {
            source = ChatSourceClassifier.classify(true, text, playerName);
        } else {
            source = playerName.isEmpty() ? ChatSource.SERVER : ChatSource.PLAYER;
        }
        if (!groupMessage && outgoing != null && outgoing.privateMessage && outgoingMatch
                && !ChatRuleMatcher.matches(EnhancedChatConfig.serverSourcePattern, text)) {
            source = ChatSource.PRIVATE;
        }
        if (source == ChatSource.SERVER) {
            playerId = null;
            playerName = "";
        }
        String privatePeer = "";
        String privateBody = "";
        if (source == ChatSource.PRIVATE) {
            privatePeer = outgoing != null && outgoing.privateMessage && outgoingMatch
                    ? selfTargetName(outgoing.target) : playerName;
            privateBody = outgoing != null && outgoing.privateMessage && outgoingMatch
                    ? outgoing.body : privateBody(component);
        }
        ChatMessageMetadata metadata = new ChatMessageMetadata(
                System.currentTimeMillis(), source, playerName, playerId, privatePeer,
                outgoingMatch, privateBody, group);
        ChatMessageMetadataRegistry.put(component, metadata);

        if (blocked(metadata, text)) {
            event.setCanceled(true);
            return;
        }
        if (EnhancedChatConfig.mentionNotification && ChatRuleMatcher.mentioned(text, localPlayerName())
                && shouldPlayMention(text, now)) playMentionSound();
        IChatComponent decorated = decoratePlayers(component, playerName);
        if (decorated != component) {
            ChatMessageMetadataRegistry.copy(component, decorated);
            event.message = decorated;
        }
    }

    static boolean blocked(ChatMessageMetadata metadata, String text) {
        if (metadata.source == ChatSource.PLAYER && EnhancedChatConfig.blockPlayerMessages
                || metadata.source == ChatSource.SERVER && EnhancedChatConfig.blockServerMessages
                || metadata.source == ChatSource.PRIVATE && EnhancedChatConfig.blockPrivateMessages) return true;
        return ChatRuleMatcher.containsName(EnhancedChatConfig.mutedPlayers, metadata.playerName)
                || ChatRuleMatcher.matches(EnhancedChatConfig.blockedMessagePattern, text);
    }

    private static boolean isGroupMessage(IChatComponent component) {
        for (Iterator<?> parts = component.iterator(); parts.hasNext();) {
            IChatComponent part = (IChatComponent) parts.next();
            if (part instanceof ChatComponentTranslation) {
                String key = ((ChatComponentTranslation) part).getKey();
                if (GROUP_INCOMING.equals(key) || GROUP_OUTGOING.equals(key)) return true;
            }
        }
        return false;
    }

    /** Returns [group, sender, outgoing] parsed from the group message component. */
    private static String[] groupParts(IChatComponent component) {
        for (Iterator<?> parts = component.iterator(); parts.hasNext();) {
            IChatComponent part = (IChatComponent) parts.next();
            if (!(part instanceof ChatComponentTranslation)) continue;
            ChatComponentTranslation translation = (ChatComponentTranslation) part;
            String key = translation.getKey();
            if (!GROUP_INCOMING.equals(key) && !GROUP_OUTGOING.equals(key)) continue;
            Object[] arguments = translation.getFormatArgs();
            if (arguments.length < 3) break;
            String sender = arguments[0] instanceof IChatComponent
                    ? ((IChatComponent) arguments[0]).getUnformattedText()
                    : String.valueOf(arguments[0]);
            String group = arguments[1] instanceof String
                    ? (String) arguments[1] : String.valueOf(arguments[1]);
            return new String[] {group, sender, GROUP_OUTGOING.equals(key) ? "1" : "0"};
        }
        return new String[] {"", "", "0"};
    }

    private static String playerName(UUID id) {
        if (id == null) return "";
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld != null) {
            for (Object entry : minecraft.theWorld.playerEntities) {
                EntityPlayer player = (EntityPlayer) entry;
                if (id.equals(player.getUniqueID())) return player.getCommandSenderName();
            }
        }
        if (minecraft.thePlayer != null && id.equals(minecraft.thePlayer.getUniqueID())) {
            return minecraft.thePlayer.getCommandSenderName();
        }
        return "";
    }

    private static String localPlayerName() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.thePlayer == null ? "" : minecraft.thePlayer.getCommandSenderName();
    }

    private static UUID localPlayerId() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.thePlayer == null ? null : minecraft.thePlayer.getUniqueID();
    }

    private static String applyActivePrivateCommand(String message) {
        if (!EnhancedChatConfigAccess.tabbedChatEnabled()
                || TabbyChat.getInstance().getChat() == null || message == null) {
            return message;
        }
        Channel active = TabbyChat.getInstance().getChat().getActiveChannel();
        if (active != null && active.isPm() && !active.getPrefix().isEmpty()
                && !message.trim().startsWith(active.getPrefix().trim())) {
            return active.getPrefix() + " " + message;
        }
        return message;
    }

    private static boolean isUnsupportedSelfMessage(ChatOutgoingMessage outgoing) {
        return outgoing != null && outgoing.privateMessage
                && (outgoing.target.equalsIgnoreCase(localPlayerName())
                    || outgoing.target.equalsIgnoreCase("@s"))
                && !SelfMessageCapability.isServerSupported();
    }

    private static String selfTargetName(String target) {
        return "@s".equalsIgnoreCase(target) ? localPlayerName() : target;
    }

    private void displayLocalSelfMessage(String body) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.ingameGUI == null) return;
        ChatComponentTranslation component = new ChatComponentTranslation(
                "commands.message.display.outgoing", minecraft.thePlayer.getDisplayName(),
                new ChatComponentText(body));
        component.getChatStyle().setColor(EnumChatFormatting.GRAY).setItalic(true);
        ChatMessageMetadata metadata = new ChatMessageMetadata(
                System.currentTimeMillis(), ChatSource.PRIVATE,
                minecraft.thePlayer.getCommandSenderName(), minecraft.thePlayer.getUniqueID(),
                minecraft.thePlayer.getCommandSenderName(), true, body);
        ChatMessageMetadataRegistry.put(component, metadata);
        IChatComponent decorated = decoratePlayers(component, minecraft.thePlayer.getCommandSenderName());
        if (decorated != component) ChatMessageMetadataRegistry.copy(component, decorated);
        minecraft.ingameGUI.getChatGUI().printChatMessage(decorated);
        long now = System.currentTimeMillis();
        if (EnhancedChatConfig.mentionNotification
                && ChatRuleMatcher.mentioned(body, minecraft.thePlayer.getCommandSenderName())
                && shouldPlayMention(decorated.getUnformattedText(), now)) {
            playMentionSound();
        }
    }

    private static IChatComponent decoratePlayers(IChatComponent component, String sender) {
        return ChatMentionDecorator.decorate(ChatPlayerLinkDecorator.decorate(component, sender));
    }

    private static String privateBody(IChatComponent component) {
        for (Iterator<?> parts = component.iterator(); parts.hasNext();) {
            IChatComponent part = (IChatComponent) parts.next();
            if (!(part instanceof ChatComponentTranslation)) continue;
            Object[] arguments = ((ChatComponentTranslation) part).getFormatArgs();
            if (arguments.length == 0) continue;
            Object body = arguments[arguments.length - 1];
            if (body instanceof IChatComponent) {
                return ((IChatComponent) body).getUnformattedText().trim();
            }
            if (body != null) return String.valueOf(body).trim();
        }
        String text = component.getUnformattedText();
        int colon = text.indexOf('\uFF1A');
        int asciiColon = text.indexOf(':');
        if (colon < 0 || asciiColon >= 0 && asciiColon < colon) colon = asciiColon;
        return colon >= 0 && colon + 1 < text.length()
                ? text.substring(colon + 1).trim() : text.trim();
    }

    private boolean shouldPlayMention(String text, long now) {
        if (text.equals(lastMentionText) && now - lastMentionSoundAt < 300L) return false;
        lastMentionText = text;
        lastMentionSoundAt = now;
        return true;
    }

    private static void playMentionSound() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            ResourceLocation sound = new ResourceLocation(EnhancedChatConfig.mentionSound);
            if (minecraft.getSoundHandler().getSound(sound) != null) {
                minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(sound, 1.0F));
            }
        } catch (RuntimeException exception) {
            NfrUiEnhancements.LOGGER.warn("Could not play chat mention sound", exception);
        }
    }
}
