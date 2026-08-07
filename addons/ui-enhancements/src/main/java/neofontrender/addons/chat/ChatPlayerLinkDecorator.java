package neofontrender.addons.chat;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Marks exact sender-name components as local player links without executing a server command. */
final class ChatPlayerLinkDecorator {
    private ChatPlayerLinkDecorator() {}

    static IChatComponent decorate(IChatComponent source, String player) {
        if (source == null || player == null || player.isEmpty()) return source;
        Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_@])" + Pattern.quote(player)
                + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
        String tooltip = AddonI18n.tr("neofontrender_ui_enhancements.chat.player.tooltip")
                .replace("%s", player);
        return decorateNode(source, player, pattern, tooltip);
    }

    private static IChatComponent decorateNode(IChatComponent source, String player,
                                               Pattern pattern, String tooltip) {
        IChatComponent copy;
        if (source instanceof ChatComponentText) {
            copy = decorateString((ChatComponentText) source, player, pattern, tooltip);
        } else if (source instanceof ChatComponentTranslation) {
            ChatComponentTranslation translation = (ChatComponentTranslation) source;
            Object[] arguments = translation.getFormatArgs().clone();
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof IChatComponent) {
                    arguments[index] = decorateNode(
                            (IChatComponent) arguments[index], player, pattern, tooltip);
                }
            }
            copy = new ChatComponentTranslation(translation.getKey(), arguments);
            copy.setChatStyle(source.getChatStyle().createShallowCopy());
        } else {
            copy = source.createCopy();
            copy.getSiblings().clear();
        }
        for (IChatComponent sibling : source.getSiblings()) {
            copy.appendSibling(decorateNode(sibling, player, pattern, tooltip));
        }
        return copy;
    }

    private static IChatComponent decorateString(ChatComponentText source, String player,
                                                 Pattern pattern, String tooltip) {
        String text = source.getChatComponentText_TextValue();
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            IChatComponent copy = source.createCopy();
            copy.getSiblings().clear();
            return copy;
        }
        ChatComponentText wrapper = new ChatComponentText("");
        int end = 0;
        do {
            if (matcher.start() > end) {
                wrapper.appendSibling(fragment(text.substring(end, matcher.start()), source.getChatStyle()));
            }
            ChatComponentText link = new ChatComponentText(text.substring(matcher.start(), matcher.end()));
            link.setChatStyle(source.getChatStyle().createShallowCopy()
                    .setUnderlined(true)
                    .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ChatComponentText(tooltip)))
                    .setChatClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                            ChatPlayerLinks.MARKER + player)));
            wrapper.appendSibling(link);
            end = matcher.end();
        } while (matcher.find());
        if (end < text.length()) {
            wrapper.appendSibling(fragment(text.substring(end), source.getChatStyle()));
        }
        return wrapper;
    }

    private static IChatComponent fragment(String text, ChatStyle sourceStyle) {
        return new ChatComponentText(text).setChatStyle(sourceStyle.createShallowCopy());
    }
}
