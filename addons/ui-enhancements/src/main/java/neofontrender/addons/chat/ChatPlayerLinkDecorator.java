package neofontrender.addons.chat;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Marks exact sender-name components as local player links without executing a server command. */
final class ChatPlayerLinkDecorator {
    private ChatPlayerLinkDecorator() {}

    static ITextComponent decorate(ITextComponent source, String player) {
        if (source == null || player == null || player.isEmpty()) return source;
        Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_@])" + Pattern.quote(player)
                + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE);
        String tooltip = AddonI18n.tr("neofontrender_ui_enhancements.chat.player.tooltip")
                .replace("%s", player);
        return decorateNode(source, player, pattern, tooltip);
    }

    private static ITextComponent decorateNode(ITextComponent source, String player,
                                               Pattern pattern, String tooltip) {
        ITextComponent copy;
        if (source instanceof TextComponentString) {
            copy = decorateString((TextComponentString) source, player, pattern, tooltip);
        } else if (source instanceof TextComponentTranslation) {
            TextComponentTranslation translation = (TextComponentTranslation) source;
            Object[] arguments = translation.getFormatArgs().clone();
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof ITextComponent) {
                    arguments[index] = decorateNode(
                            (ITextComponent) arguments[index], player, pattern, tooltip);
                }
            }
            copy = new TextComponentTranslation(translation.getKey(), arguments);
            copy.setStyle(source.getStyle().createShallowCopy());
        } else {
            copy = source.createCopy();
            copy.getSiblings().clear();
        }
        for (ITextComponent sibling : source.getSiblings()) {
            copy.appendSibling(decorateNode(sibling, player, pattern, tooltip));
        }
        return copy;
    }

    private static ITextComponent decorateString(TextComponentString source, String player,
                                                 Pattern pattern, String tooltip) {
        String text = source.getText();
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            ITextComponent copy = source.createCopy();
            copy.getSiblings().clear();
            return copy;
        }
        TextComponentString wrapper = new TextComponentString("");
        int end = 0;
        do {
            if (matcher.start() > end) {
                wrapper.appendSibling(fragment(text.substring(end, matcher.start()), source.getStyle()));
            }
            TextComponentString link = new TextComponentString(text.substring(matcher.start(), matcher.end()));
            link.setStyle(source.getStyle().createShallowCopy()
                    .setUnderlined(true)
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString(tooltip)))
                    .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                            ChatPlayerLinks.MARKER + player)));
            wrapper.appendSibling(link);
            end = matcher.end();
        } while (matcher.find());
        if (end < text.length()) {
            wrapper.appendSibling(fragment(text.substring(end), source.getStyle()));
        }
        return wrapper;
    }

    private static ITextComponent fragment(String text, Style sourceStyle) {
        return new TextComponentString(text).setStyle(sourceStyle.createShallowCopy());
    }
}
