package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.HoverEvent;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adds local-only styling and hover text to exact online-player mentions. */
final class ChatMentionDecorator {
    private static final Pattern MENTION = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])@([A-Za-z0-9_]{1,16})(?![\\p{L}\\p{N}_])");

    private ChatMentionDecorator() {}

    static IChatComponent decorate(IChatComponent source) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (source == null) return source;
        NetHandlerPlayClient connection = minecraft.getNetHandler();
        if (connection == null) return source;
        Map<String, String> players = new LinkedHashMap<>();
        for (GuiPlayerInfo player : connection.playerInfoList) {
            String name = player.name;
            players.put(name.toLowerCase(Locale.ROOT), name);
        }
        String localName = minecraft.thePlayer == null ? "" : minecraft.thePlayer.getCommandSenderName();
        return decorate(source, players, localName,
                AddonI18n.tr("neofontrender_ui_enhancements.chat.mention.tooltip"));
    }

    static IChatComponent decorate(IChatComponent source, Map<String, String> players,
                                   String localName, String tooltipPattern) {
        if (source == null || players == null || players.isEmpty()) return source;
        return decorateNode(source, players, localName == null ? "" : localName,
                tooltipPattern == null ? "%s" : tooltipPattern);
    }

    private static IChatComponent decorateNode(IChatComponent source, Map<String, String> players,
                                                String localName, String tooltipPattern) {
        IChatComponent copy;
        if (source instanceof ChatComponentText) {
            copy = decorateString((ChatComponentText) source, players, localName, tooltipPattern);
        } else if (source instanceof ChatComponentTranslation) {
            ChatComponentTranslation translation = (ChatComponentTranslation) source;
            Object[] original = translation.getFormatArgs();
            Object[] arguments = original.clone();
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof IChatComponent) {
                    arguments[index] = decorateNode((IChatComponent) arguments[index], players,
                            localName, tooltipPattern);
                }
            }
            copy = new ChatComponentTranslation(translation.getKey(), arguments);
            copy.setChatStyle(source.getChatStyle().createShallowCopy());
        } else {
            copy = source.createCopy();
            copy.getSiblings().clear();
        }
        for (IChatComponent sibling : source.getSiblings()) {
            copy.appendSibling(decorateNode(sibling, players, localName, tooltipPattern));
        }
        return copy;
    }

    private static IChatComponent decorateString(ChatComponentText source, Map<String, String> players,
                                                 String localName, String tooltipPattern) {
        String text = source.getChatComponentText_TextValue();
        Matcher matcher = MENTION.matcher(text);
        if (!matcher.find()) {
            IChatComponent copy = source.createCopy();
            copy.getSiblings().clear();
            return copy;
        }
        ChatComponentText wrapper = new ChatComponentText("");
        int end = 0;
        do {
            String canonical = players.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (canonical == null) continue;
            if (matcher.start() > end) wrapper.appendSibling(fragment(
                    text.substring(end, matcher.start()), source.getChatStyle()));
            ChatComponentText mention = new ChatComponentText(text.substring(matcher.start(), matcher.end()));
            ChatStyle style = source.getChatStyle().createShallowCopy()
                    .setColor(canonical.equalsIgnoreCase(localName) ? EnumChatFormatting.GOLD : EnumChatFormatting.AQUA)
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(tooltipPattern.replace("%s", canonical))));
            mention.setChatStyle(style);
            wrapper.appendSibling(mention);
            end = matcher.end();
        } while (matcher.find());
        if (end == 0) {
            IChatComponent copy = source.createCopy();
            copy.getSiblings().clear();
            return copy;
        }
        if (end < text.length()) wrapper.appendSibling(fragment(text.substring(end), source.getChatStyle()));
        return wrapper;
    }

    private static IChatComponent fragment(String text, ChatStyle sourceStyle) {
        return new ChatComponentText(text).setChatStyle(sourceStyle.createShallowCopy());
    }
}
