package neofontrender.addons.inline;

import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.InlineContentMatch;
import neofontrender.api.text.pipeline.InlineContentMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;
import neofontrender.addons.chat.EnhancedChatFeatures;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.net.URI;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Gosling-compatible aliases and Discord custom-emoji tags. Unicode stays in the font pipeline. */
final class GoslingEmojiContentMiddleware implements InlineContentMiddleware {
    private static final Pattern DISCORD = Pattern.compile("<a?:([\\w]+):([a-zA-Z0-9+/=]+)>");
    private static final int MAX_ALIAS = 96;

    @Override public boolean isEnabled() { return EnhancedChatFeatures.goslingImageGlyphs(); }

    @Nullable
    @Override
    public InlineContentMatch match(CharSequence source, int sourceIndex) {
        return matchEnabled(source, sourceIndex);
    }

    @Nullable
    static InlineContentMatch matchEnabled(CharSequence source, int sourceIndex) {
        char first = source.charAt(sourceIndex);
        if (first == '<') {
            InlineContentMatch discord = discord(source, sourceIndex);
            if (discord != null) return discord;
        }
        if (first == ':') {
            InlineContentMatch alias = alias(source, sourceIndex);
            if (alias != null) return alias;
        }
        // Raw Unicode emoji deliberately falls through to ModernTextApi. The configured emoji
        // font/fallback family then owns color glyphs, metrics and style selection.
        return null;
    }

    @Nullable
    private static InlineContentMatch alias(CharSequence source, int start) {
        int limit = Math.min(source.length(), start + MAX_ALIAS);
        for (int end = start + 1; end < limit; end++) {
            if (source.charAt(end) != ':') continue;
            String name = source.subSequence(start + 1, end).toString();
            GoslingEmojiCatalog.Entry entry = GoslingEmojiCatalog.INSTANCE.alias(name);
            if (entry != null) return standard(start, end + 1, entry, ":" + name + ":");
        }
        return null;
    }

    @Nullable
    private static InlineContentMatch discord(CharSequence source, int start) {
        int end = -1;
        for (int i = start + 1; i < Math.min(source.length(), start + MAX_ALIAS); i++) {
            if (source.charAt(i) == '>') { end = i + 1; break; }
        }
        if (end < 0) return null;
        Matcher matcher = DISCORD.matcher(source.subSequence(start, end));
        if (!matcher.matches()) return null;
        Long id = decodeDiscordId(matcher.group(2));
        if (id == null || id <= 0) return null;
        URI uri = URI.create("https://cdn.discordapp.com/emojis/" + id);
        InlineContent glyph = InlineImageService.INSTANCE.glyph(uri,
                ":" + matcher.group(1) + ": · cdn.discordapp.com", true);
        return glyph == null ? null : new InlineContentMatch(start, end, glyph);
    }

    @Nullable
    private static Long decodeDiscordId(String value) {
        if (value.matches("[0-9]{13,20}")) {
            try { return Long.parseLong(value); } catch (NumberFormatException ignored) { return null; }
        }
        try {
            return new BigInteger(Base64.getDecoder().decode(value)).longValueExact();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nullable
    private static InlineContentMatch standard(int start, int end,
                                             GoslingEmojiCatalog.Entry entry, String source) {
        InlineContent glyph = InlineImageService.INSTANCE.glyph(entry.uri,
                source + " · " + entry.uri.getHost(), true);
        return glyph == null ? null : new InlineContentMatch(start, end, glyph);
    }

    @Override public String id() { return "neofontrender_ui_enhancements:gosling_emoji"; }
    @Override public int priority() { return 50; }
    @Override public TextTrigger trigger() { return TextTrigger.exact('<', ':'); }
}
