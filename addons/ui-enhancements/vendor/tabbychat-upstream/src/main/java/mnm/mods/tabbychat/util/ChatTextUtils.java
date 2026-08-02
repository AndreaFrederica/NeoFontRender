package mnm.mods.tabbychat.util;

import com.google.common.collect.Lists;
import mnm.mods.tabbychat.ChatMessage;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.chat.ChatItemIconRenderer;
import neofontrender.addons.chat.ChatMessageMetadata;
import neofontrender.addons.chat.ChatMessageMetadataRegistry;
import neofontrender.addons.chat.ChatSource;

public class ChatTextUtils {

    public static List<ITextComponent> split(ITextComponent chat, int width) {
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        return GuiUtilRenderComponents.splitText(ChatItemIconRenderer.decorate(chat), width, fr, false, false);
    }

    public static List<Message> split(List<Message> list, int width) {
        return split(list, width, false);
    }

    public static List<Message> split(List<Message> list, int width, boolean privateView) {
        if (width <= 8) // ignore, characters are larger than width
            return Lists.newArrayList(list);
        // prevent concurrent modification caused by chat thread
        synchronized (list) {
            List<Message> result = Lists.newArrayList();
            Iterator<Message> iter = list.iterator();
            while (iter.hasNext() && result.size() <= 100) {
                Message line = iter.next();
                ChatMessageMetadata metadata = line instanceof ChatMessage
                        ? ((ChatMessage) line).nfrUi$getMessageMetadata() : null;
                ITextComponent display = line.getMessageWithOptionalTimestamp();
                if (privateView && metadata != null && metadata.source == ChatSource.PRIVATE) {
                    ITextComponent body = findPrivateBody(line.getMessage());
                    if (body == null && !metadata.privateBody.isEmpty()) {
                        body = new TextComponentString(metadata.privateBody);
                    }
                    if (body != null && line.getDate() != null
                            && TabbyChat.getInstance().settings.general.timestampChat.get()) {
                        body = new TextComponentString(
                                TabbyChat.getInstance().settings.general.timestampColor.get()
                                + TabbyChat.getInstance().settings.general.timestampStyle.get().format(line.getDate())
                                + TextFormatting.RESET + " ").appendSibling(body);
                    }
                    if (body != null) display = body;
                }
                List<ITextComponent> chatlist = split(display, width);
                UUID senderId = line instanceof ChatMessage
                        ? ((ChatMessage) line).nfrUi$getSenderId() : ChatHeadResolver.detect(line.getMessage());
                for (int i = chatlist.size() - 1; i >= 0; i--) {
                    ITextComponent chat = chatlist.get(i);
                    ChatMessageMetadata fragmentMetadata = metadata;
                    if (privateView && metadata != null && !metadata.outgoing
                            && isOutgoingPrivate(line.getMessage())) {
                        fragmentMetadata = new ChatMessageMetadata(metadata.timestamp,
                                metadata.source, metadata.playerName, metadata.playerId,
                                metadata.privatePeer, true, display.getUnformattedText());
                    }
                    ChatMessageMetadataRegistry.put(chat, fragmentMetadata);
                    result.add(new ChatMessage(line.getCounter(), chat, line.getID(), false,
                            senderId, i == 0));
                }
            }
            return result;
        }
    }

    private static ITextComponent findPrivateBody(ITextComponent component) {
        if (component instanceof TextComponentTranslation) {
            Object[] arguments = ((TextComponentTranslation) component).getFormatArgs();
            if (arguments.length > 0) {
                Object body = arguments[arguments.length - 1];
                if (body instanceof ITextComponent) return ((ITextComponent) body).createCopy();
                if (body != null) return new TextComponentString(String.valueOf(body));
            }
        }
        for (ITextComponent sibling : component.getSiblings()) {
            ITextComponent body = findPrivateBody(sibling);
            if (body != null) return body;
        }
        return null;
    }

    private static boolean isOutgoingPrivate(ITextComponent component) {
        if (component instanceof TextComponentTranslation
                && "commands.message.display.outgoing".equals(
                ((TextComponentTranslation) component).getKey())) return true;
        for (ITextComponent sibling : component.getSiblings()) {
            if (isOutgoingPrivate(sibling)) return true;
        }
        return false;
    }

    /**
     * Returns a ChatComponent that is a sub-component of another one. It begins
     * at the specified index and extends to the end of the componenent.
     *
     * @param chat The chat to subchat
     * @param beginIndex The beginning index, inclusive
     * @return The end of the chat
     * @see String#substring(int)
     */
    public static ITextComponent subChat(ITextComponent chat, int beginIndex) {
        ITextComponent rchat = null;
        Iterator<ITextComponent> ichat = chat.iterator();
        int pos = 0;
        while (ichat.hasNext()) {
            ITextComponent part = ichat.next();
            String s = part.getUnformattedComponentText();

            int len = s.length();
            if (len + pos >= beginIndex) {
                if (pos < beginIndex) {
                    ITextComponent schat = new TextComponentString(s.substring(beginIndex - pos));
                    schat.setStyle(part.getStyle().createShallowCopy());
                    part = schat;
                }
                if (rchat == null) {
                    rchat = part;
                } else {
                    rchat.appendSibling(part);
                }
            }
            pos += len;
        }
        return rchat;
    }
}
