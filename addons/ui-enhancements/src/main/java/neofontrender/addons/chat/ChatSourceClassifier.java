package neofontrender.addons.chat;


final class ChatSourceClassifier {
    private static final String DEFAULT_PRIVATE = "(?i)^(?:from\\s+|to\\s+|\\[.*?\\s*-?>\\s*me]|.*?\\s+whispers to you:|you whisper to\\s+|.*?悄悄地对你说|你悄悄地对.*?)";

    private ChatSourceClassifier() {}

    static ChatSource classify(boolean vanillaPlayerType, String text, String detectedPlayer,
                               String privatePattern, String serverPattern, String playerPattern) {
        if (ChatRuleMatcher.matches(privatePattern, text)
                || ChatRuleMatcher.matches(DEFAULT_PRIVATE, text)) return ChatSource.PRIVATE;
        if (ChatRuleMatcher.matches(serverPattern, text)) return ChatSource.SERVER;
        if (ChatRuleMatcher.matches(playerPattern, text)) return ChatSource.PLAYER;
        return vanillaPlayerType && detectedPlayer != null && !detectedPlayer.isEmpty()
                ? ChatSource.PLAYER : ChatSource.SERVER;
    }

    static ChatSource classify(boolean vanillaPlayerType, String text, String detectedPlayer) {
        return classify(vanillaPlayerType, text, detectedPlayer,
                EnhancedChatConfig.privateSourcePattern,
                EnhancedChatConfig.serverSourcePattern,
                EnhancedChatConfig.playerSourcePattern);
    }
}
