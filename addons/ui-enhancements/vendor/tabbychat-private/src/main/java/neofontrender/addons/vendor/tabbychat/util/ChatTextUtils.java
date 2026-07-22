package neofontrender.addons.vendor.tabbychat.util;

import com.google.common.collect.Lists;
import neofontrender.addons.vendor.tabbychat.ChatMessage;
import neofontrender.addons.vendor.tabbychat.api.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;

import java.util.Iterator;
import java.util.List;

public class ChatTextUtils {

    public static List<IChatComponent> split(IChatComponent chat, int width) {
        if (chat == null) {
            throw new IllegalArgumentException("chat");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        List<IChatComponent> pending = Lists.newArrayList(chat);
        List<IChatComponent> lines = Lists.newArrayList();
        ChatComponentText current = new ChatComponentText("");
        int currentWidth = 0;

        for (int index = 0; index < pending.size(); index++) {
            IChatComponent part = pending.get(index);
            String text = part.getChatStyle().getFormattingCode() + part.getUnformattedTextForChat();
            text = stripFormattingIfDisabled(text);
            int partWidth = fr.getStringWidth(text);
            ChatComponentText rendered = component(text, part);
            boolean wrapped = false;

            if (currentWidth + partWidth > width) {
                String head = fr.trimStringToWidth(text, width - currentWidth, false);
                if (head.isEmpty() && !text.isEmpty()) {
                    head = text.substring(0, 1);
                }
                String tail = head.length() < text.length() ? text.substring(head.length()) : null;
                if (tail != null && !tail.isEmpty()) {
                    int space = head.lastIndexOf(' ');
                    if (space >= 0 && fr.getStringWidth(text.substring(0, space)) > 0) {
                        head = text.substring(0, space);
                        tail = text.substring(space);
                    }
                    pending.add(index + 1, component(tail, part));
                }
                partWidth = fr.getStringWidth(head);
                rendered = component(head, part);
                wrapped = true;
            }

            if (currentWidth + partWidth <= width) {
                currentWidth += partWidth;
                current.appendSibling(rendered);
            } else {
                wrapped = true;
            }

            if (wrapped) {
                lines.add(current);
                current = new ChatComponentText("");
                currentWidth = 0;
            }
        }
        lines.add(current);
        return lines;
    }

    public static String stripFormattingIfDisabled(String text) {
        return Minecraft.getMinecraft().gameSettings.chatColours
                ? text
                : EnumChatFormatting.getTextWithoutFormattingCodes(text);
    }

    private static ChatComponentText component(String text, IChatComponent source) {
        ChatComponentText component = new ChatComponentText(text);
        component.setChatStyle(source.getChatStyle().createShallowCopy());
        return component;
    }

    public static List<Message> split(List<Message> list, int width) {
        if (width <= 8) // ignore, characters are larger than width
            return Lists.newArrayList(list);
        // prevent concurrent modification caused by chat thread
        synchronized (list) {
            List<Message> result = Lists.newArrayList();
            Iterator<Message> iter = list.iterator();
            while (iter.hasNext() && result.size() <= 100) {
                Message line = iter.next();
                List<IChatComponent> chatlist = split(line.getMessageWithOptionalTimestamp(), width);
                for (int i = chatlist.size() - 1; i >= 0; i--) {
                    IChatComponent chat = chatlist.get(i);
                    result.add(new ChatMessage(line.getCounter(), chat, line.getID(), false));
                }
            }
            return result;
        }
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
    public static IChatComponent subChat(IChatComponent chat, int beginIndex) {
        IChatComponent rchat = null;
        Iterator<IChatComponent> ichat = chat.iterator();
        int pos = 0;
        while (ichat.hasNext()) {
            IChatComponent part = ichat.next();
            String s = part.getUnformattedTextForChat();

            int len = s.length();
            if (len + pos >= beginIndex) {
                if (pos < beginIndex) {
                    IChatComponent schat = new ChatComponentText(s.substring(beginIndex - pos));
                    schat.setChatStyle(part.getChatStyle().createShallowCopy());
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
