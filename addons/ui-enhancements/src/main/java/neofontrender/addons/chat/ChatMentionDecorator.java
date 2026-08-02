package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
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

    static ITextComponent decorate(ITextComponent source) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (source == null || minecraft.getConnection() == null) return source;
        Map<String, String> players = new LinkedHashMap<>();
        for (NetworkPlayerInfo player : minecraft.getConnection().getPlayerInfoMap()) {
            String name = player.getGameProfile().getName();
            players.put(name.toLowerCase(Locale.ROOT), name);
        }
        String localName = minecraft.player == null ? "" : minecraft.player.getName();
        return decorate(source, players, localName,
                AddonI18n.tr("neofontrender_ui_enhancements.chat.mention.tooltip"));
    }

    static ITextComponent decorate(ITextComponent source, Map<String, String> players,
                                   String localName, String tooltipPattern) {
        if (source == null || players == null || players.isEmpty()) return source;
        return decorateNode(source, players, localName == null ? "" : localName,
                tooltipPattern == null ? "%s" : tooltipPattern);
    }

    private static ITextComponent decorateNode(ITextComponent source, Map<String, String> players,
                                                String localName, String tooltipPattern) {
        ITextComponent copy;
        if (source instanceof TextComponentString) {
            copy = decorateString((TextComponentString) source, players, localName, tooltipPattern);
        } else if (source instanceof TextComponentTranslation) {
            TextComponentTranslation translation = (TextComponentTranslation) source;
            Object[] original = translation.getFormatArgs();
            Object[] arguments = original.clone();
            for (int index = 0; index < arguments.length; index++) {
                if (arguments[index] instanceof ITextComponent) {
                    arguments[index] = decorateNode((ITextComponent) arguments[index], players,
                            localName, tooltipPattern);
                }
            }
            copy = new TextComponentTranslation(translation.getKey(), arguments);
            copy.setStyle(source.getStyle().createShallowCopy());
        } else {
            copy = source.createCopy();
            copy.getSiblings().clear();
        }
        for (ITextComponent sibling : source.getSiblings()) {
            copy.appendSibling(decorateNode(sibling, players, localName, tooltipPattern));
        }
        return copy;
    }

    private static ITextComponent decorateString(TextComponentString source, Map<String, String> players,
                                                 String localName, String tooltipPattern) {
        String text = source.getText();
        Matcher matcher = MENTION.matcher(text);
        if (!matcher.find()) {
            ITextComponent copy = source.createCopy();
            copy.getSiblings().clear();
            return copy;
        }
        TextComponentString wrapper = new TextComponentString("");
        int end = 0;
        do {
            String canonical = players.get(matcher.group(1).toLowerCase(Locale.ROOT));
            if (canonical == null) continue;
            if (matcher.start() > end) wrapper.appendSibling(fragment(
                    text.substring(end, matcher.start()), source.getStyle()));
            TextComponentString mention = new TextComponentString(text.substring(matcher.start(), matcher.end()));
            Style style = source.getStyle().createShallowCopy()
                    .setColor(canonical.equalsIgnoreCase(localName) ? TextFormatting.GOLD : TextFormatting.AQUA)
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString(tooltipPattern.replace("%s", canonical))));
            mention.setStyle(style);
            wrapper.appendSibling(mention);
            end = matcher.end();
        } while (matcher.find());
        if (end == 0) {
            ITextComponent copy = source.createCopy();
            copy.getSiblings().clear();
            return copy;
        }
        if (end < text.length()) wrapper.appendSibling(fragment(text.substring(end), source.getStyle()));
        return wrapper;
    }

    private static ITextComponent fragment(String text, Style sourceStyle) {
        return new TextComponentString(text).setStyle(sourceStyle.createShallowCopy());
    }
}
