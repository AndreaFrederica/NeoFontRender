package neofontrender.addons.chat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class ChatSearchQuery {
    private static final Pattern TOKENS = Pattern.compile("\\\"([^\\\"]*)\\\"|(\\S+)");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private final List<String> terms = new ArrayList<>();
    private ChatSource source;
    private String player = "";
    private LocalTime after;
    private LocalTime before;
    private Pattern expression;
    private boolean invalid;

    static ChatSearchQuery parse(String value) {
        ChatSearchQuery query = new ChatSearchQuery();
        Matcher matcher = TOKENS.matcher(value == null ? "" : value.trim());
        while (matcher.find()) query.add(matcher.group(1) != null ? matcher.group(1) : matcher.group(2));
        return query;
    }

    boolean matches(ChatSearchEntry entry) {
        if (invalid) return false;
        String lower = entry.text.toLowerCase(Locale.ROOT);
        for (String term : terms) if (!lower.contains(term)) return false;
        if (source != null && entry.metadata.source != source) return false;
        if (!player.isEmpty() && !entry.metadata.playerName.toLowerCase(Locale.ROOT).contains(player)) return false;
        LocalTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.metadata.timestamp),
                ZoneId.systemDefault()).toLocalTime();
        if (after != null && time.isBefore(after) || before != null && time.isAfter(before)) return false;
        return expression == null || expression.matcher(entry.text).find();
    }

    private void add(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.startsWith("type:") || lower.startsWith("source:") || lower.startsWith("来源:")) {
            String value = token.substring(token.indexOf(':') + 1).toLowerCase(Locale.ROOT);
            if ("player".equals(value) || "玩家".equals(value)) source = ChatSource.PLAYER;
            else if ("server".equals(value) || "服务器".equals(value)) source = ChatSource.SERVER;
            else if ("private".equals(value) || "pm".equals(value) || "私聊".equals(value)) source = ChatSource.PRIVATE;
            else invalid = true;
        } else if (lower.startsWith("from:") || lower.startsWith("玩家:")) {
            player = token.substring(token.indexOf(':') + 1).toLowerCase(Locale.ROOT);
        } else if (lower.startsWith("after:") || lower.startsWith("晚于:")) {
            after = parseTime(token.substring(token.indexOf(':') + 1));
            invalid |= after == null;
        } else if (lower.startsWith("before:") || lower.startsWith("早于:")) {
            before = parseTime(token.substring(token.indexOf(':') + 1));
            invalid |= before == null;
        } else if (token.length() > 2 && token.startsWith("/") && token.endsWith("/")) {
            try {
                expression = Pattern.compile(token.substring(1, token.length() - 1),
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            } catch (PatternSyntaxException exception) {
                invalid = true;
            }
        } else if (!token.isEmpty()) {
            terms.add(token.toLowerCase(Locale.ROOT));
        }
    }

    private static LocalTime parseTime(String value) {
        try { return LocalTime.parse(value, TIME); }
        catch (DateTimeParseException ignored) { return null; }
    }
}
