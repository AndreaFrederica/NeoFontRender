package neofontrender.addons.chat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Short-lived description of a locally sent message used to identify its server echo. */
final class ChatOutgoingMessage {
    private static final long MAX_ECHO_DELAY_MS = 8_000L;
    private static final Set<String> PRIVATE_COMMANDS = new HashSet<>(Arrays.asList(
            "/msg", "/tell", "/w", "/whisper", "/message", "/m"));

    final String body;
    final String target;
    final boolean privateMessage;
    final long sentAt;

    private ChatOutgoingMessage(String body, String target, boolean privateMessage, long sentAt) {
        this.body = body;
        this.target = target;
        this.privateMessage = privateMessage;
        this.sentAt = sentAt;
    }

    static ChatOutgoingMessage parse(String message, String privateTemplate, long now) {
        if (message == null || message.trim().isEmpty()) return null;
        String trimmed = message.trim();
        if (!trimmed.startsWith("/")) return new ChatOutgoingMessage(trimmed, "", false, now);
        Set<String> commands = new HashSet<>(PRIVATE_COMMANDS);
        if (privateTemplate != null && privateTemplate.trim().startsWith("/")) {
            commands.add(privateTemplate.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT));
        }
        String[] tokens = trimmed.split("\\s+", 3);
        if (tokens.length < 3 || !commands.contains(tokens[0].toLowerCase(Locale.ROOT))) return null;
        return new ChatOutgoingMessage(tokens[2], tokens[1], true, now);
    }

    boolean matches(String receivedText, long now) {
        return receivedText != null && now - sentAt >= 0L && now - sentAt <= MAX_ECHO_DELAY_MS
                && !body.isEmpty() && receivedText.contains(body);
    }
}
