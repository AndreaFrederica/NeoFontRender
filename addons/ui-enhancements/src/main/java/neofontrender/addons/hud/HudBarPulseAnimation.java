package neofontrender.addons.hud;

/** ClassicBar-style sine pulse used by held-food preview overlays. */
final class HudBarPulseAnimation {
    private static final double SPEED = 3.0D;

    private HudBarPulseAnimation() {}

    static int alpha(long nowNanos) {
        double seconds = nowNanos / 1_000_000_000.0D;
        return Math.round((float) ((Math.sin(seconds * SPEED) * 0.5D + 0.5D) * 255.0D));
    }
}
