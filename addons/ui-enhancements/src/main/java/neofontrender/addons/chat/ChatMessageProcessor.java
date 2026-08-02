package neofontrender.addons.chat;

import mnm.mods.tabbychat.ChatManager;
import mnm.mods.tabbychat.TabbyChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import neofontrender.addons.chat.network.SelfMessageCapability;

import java.util.UUID;

public enum ChatMessageProcessor {
    INSTANCE;

    private ChatOutgoingMessage recentOutgoing;
    private String lastMentionText = "";
    private long lastMentionSoundAt;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void sent(ClientChatEvent event) {
        String message = event.getMessage();
        if (EnhancedChatConfigAccess.tabbedChatEnabled()
                && TabbyChat.getInstance().getChat() instanceof ChatManager) {
            message = ((ChatManager) TabbyChat.getInstance().getChat())
                    .applyActivePrivateCommand(message);
            event.setMessage(message);
        }
        recentOutgoing = ChatOutgoingMessage.parse(message,
                EnhancedChatConfig.privateMessageCommand, System.currentTimeMillis());
        if (isUnsupportedSelfMessage(recentOutgoing)) {
            event.setCanceled(true);
            displayLocalSelfMessage(recentOutgoing.body);
            recentOutgoing = null;
        }
    }

    @SubscribeEvent
    public void connected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        SelfMessageCapability.resetClient();
        Minecraft.getMinecraft().addScheduledTask(SelfMessageCapability::probeServer);
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        SelfMessageCapability.resetClient();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void received(ClientChatReceivedEvent event) {
        if (!EnhancedChatConfig.enabled || event.getType() == ChatType.GAME_INFO) return;
        ITextComponent component = event.getMessage();
        UUID playerId = ChatHeadResolver.detectSender(component);
        String playerName = playerName(playerId);
        String text = component.getUnformattedText();
        long now = System.currentTimeMillis();
        ChatOutgoingMessage outgoing = recentOutgoing;
        boolean outgoingMatch = outgoing != null && outgoing.matches(text, now);
        if (outgoingMatch) {
            playerId = localPlayerId();
            playerName = localPlayerName();
        }
        ChatSource source = EnhancedChatConfig.sourceClassification
                ? ChatSourceClassifier.classify(event.getType(), text, playerName)
                : event.getType() == ChatType.CHAT ? ChatSource.PLAYER : ChatSource.SERVER;
        if (outgoing != null && outgoing.privateMessage && outgoingMatch
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
                outgoingMatch, privateBody);
        ChatMessageMetadataRegistry.put(component, metadata);

        if (blocked(metadata, text)) {
            event.setCanceled(true);
            return;
        }
        if (EnhancedChatConfig.mentionNotification && ChatRuleMatcher.mentioned(text, localPlayerName())
                && shouldPlayMention(text, now)) playMentionSound();
        ITextComponent decorated = decoratePlayers(component, playerName);
        if (decorated != component) {
            ChatMessageMetadataRegistry.copy(component, decorated);
            event.setMessage(decorated);
        }
    }

    static boolean blocked(ChatMessageMetadata metadata, String text) {
        if (metadata.source == ChatSource.PLAYER && EnhancedChatConfig.blockPlayerMessages
                || metadata.source == ChatSource.SERVER && EnhancedChatConfig.blockServerMessages
                || metadata.source == ChatSource.PRIVATE && EnhancedChatConfig.blockPrivateMessages) return true;
        return ChatRuleMatcher.containsName(EnhancedChatConfig.mutedPlayers, metadata.playerName)
                || ChatRuleMatcher.matches(EnhancedChatConfig.blockedMessagePattern, text);
    }

    private static String playerName(UUID id) {
        if (id == null || Minecraft.getMinecraft().getConnection() == null) return "";
        for (NetworkPlayerInfo player : Minecraft.getMinecraft().getConnection().getPlayerInfoMap()) {
            if (id.equals(player.getGameProfile().getId())) return player.getGameProfile().getName();
        }
        return "";
    }

    private static String localPlayerName() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player == null ? "" : minecraft.player.getName();
    }

    private static UUID localPlayerId() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player == null ? null : minecraft.player.getUniqueID();
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
        if (minecraft.player == null || minecraft.ingameGUI == null) return;
        TextComponentTranslation component = new TextComponentTranslation(
                "commands.message.display.outgoing", minecraft.player.getDisplayName(),
                new TextComponentString(body));
        component.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
        ChatMessageMetadata metadata = new ChatMessageMetadata(
                System.currentTimeMillis(), ChatSource.PRIVATE,
                minecraft.player.getName(), minecraft.player.getUniqueID(),
                minecraft.player.getName(), true, body);
        ChatMessageMetadataRegistry.put(component, metadata);
        ITextComponent decorated = decoratePlayers(component, minecraft.player.getName());
        if (decorated != component) ChatMessageMetadataRegistry.copy(component, decorated);
        minecraft.ingameGUI.getChatGUI().printChatMessage(decorated);
        long now = System.currentTimeMillis();
        if (EnhancedChatConfig.mentionNotification
                && ChatRuleMatcher.mentioned(body, minecraft.player.getName())
                && shouldPlayMention(decorated.getUnformattedText(), now)) {
            playMentionSound();
        }
    }

    private static ITextComponent decoratePlayers(ITextComponent component, String sender) {
        return ChatMentionDecorator.decorate(ChatPlayerLinkDecorator.decorate(component, sender));
    }

    private static String privateBody(ITextComponent component) {
        for (ITextComponent part : component) {
            if (!(part instanceof TextComponentTranslation)) continue;
            Object[] arguments = ((TextComponentTranslation) part).getFormatArgs();
            if (arguments.length == 0) continue;
            Object body = arguments[arguments.length - 1];
            if (body instanceof ITextComponent) {
                return ((ITextComponent) body).getUnformattedText().trim();
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
            SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(EnhancedChatConfig.mentionSound));
            if (sound != null) Minecraft.getMinecraft().getSoundHandler()
                    .playSound(PositionedSoundRecord.getMasterRecord(sound, 1.0F));
        } catch (RuntimeException ignored) {
            // Invalid user-provided sound identifiers simply disable the notification sound.
        }
    }
}
