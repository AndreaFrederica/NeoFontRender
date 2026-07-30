package neofontrender.addons.chat;

/** Vanilla-compatible chat fade calculation shared by custom chat renderers. */
public final class ChatFadeMath {
    private ChatFadeMath() {}

    public static float lineFade(int updateCounter, int lineCounter, int fadeTime) {
        if (fadeTime <= 0) return 0.0F;
        double age = Math.max(0, updateCounter - lineCounter);
        double opacity = (1.0D - age / fadeTime) * 10.0D;
        opacity = Math.max(0.0D, Math.min(1.0D, opacity));
        return (float) (opacity * opacity);
    }

    public static int lineOpacity(float chatOpacity, float lineFade) {
        float opacity = clamp(chatOpacity) * clamp(lineFade);
        return Math.round(opacity * 255.0F);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
