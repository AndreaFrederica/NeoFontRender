package neofontrender.addons.chat;

/** Shared wall-clock animation state for vanilla and embedded TabbyChat renderers. */
public final class ChatAnimationController {
    private static long lastMessageMillis = Long.MIN_VALUE;
    private static long openedMillis = Long.MIN_VALUE;
    private static boolean chatOpen;

    private ChatAnimationController() {}

    public static void messageAdded() {
        lastMessageMillis = System.currentTimeMillis();
    }

    public static void chatOpened() {
        if (chatOpen) return;
        chatOpen = true;
        openedMillis = System.currentTimeMillis();
    }

    public static void chatClosed() {
        chatOpen = false;
    }

    public static float messageOffset(boolean scrolled) {
        if (!EnhancedChatConfig.enabled || !EnhancedChatConfig.animateMessages || scrolled) return 0.0F;
        return remainingOffset(lastMessageMillis, EnhancedChatConfig.messageAnimationDuration,
                EnhancedChatConfig.messageAnimationDistance, EnhancedChatConfig.messageAnimationEasing);
    }

    public static float inputOffset() {
        if (!EnhancedChatConfig.enabled || !EnhancedChatConfig.animateInput || !chatOpen) return 0.0F;
        return remainingOffset(openedMillis, EnhancedChatConfig.inputAnimationDuration,
                EnhancedChatConfig.inputAnimationDistance, EnhancedChatConfig.inputAnimationEasing);
    }

    private static float remainingOffset(long started, int duration, float distance, String easing) {
        if (started == Long.MIN_VALUE || duration <= 0 || distance == 0.0F) return 0.0F;
        float progress = Math.min(1.0F, Math.max(0.0F,
                (System.currentTimeMillis() - started) / (float) duration));
        return distance * (1.0F - ease(progress, easing));
    }

    static float ease(float x, String easing) {
        switch (easing) {
            case "linear": return x;
            case "quad": return 1.0F - (1.0F - x) * (1.0F - x);
            case "cubic": {
                float inverse = 1.0F - x;
                return 1.0F - inverse * inverse * inverse;
            }
            case "back": {
                // Ease-out-back: settles with the same small overshoot used by ChatAnimation.
                float c1 = 1.70158F;
                float c3 = c1 + 1.0F;
                float shifted = x - 1.0F;
                return 1.0F + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
            }
            case "sine":
            default:
                return (float) (1.0D - Math.cos(x * Math.PI * 0.5D));
        }
    }
}
