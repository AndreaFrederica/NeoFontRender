package neofontrender.audio;

import java.util.Objects;
import java.util.Random;

/** Vanilla-style scheduling shared by independent playback and the Minecraft adapter.
 * Scene identity is deliberately independent of the selected track's identifier.
 * Call once per game tick, not per rendered frame.
 */
public final class SceneMusicPolicy {
    public enum Action { NONE, STOP, PLAY }
    private final Random random;
    private String scene;
    private int delay = 100;
    public SceneMusicPolicy(Random random) { this.random = Objects.requireNonNull(random); }
    public int remainingTicks() { return delay; }
    public Action tick(String currentScene, int minDelay, int maxDelay, boolean playing, boolean paused) {
        Objects.requireNonNull(currentScene);
        if (minDelay < 0 || maxDelay < minDelay || maxDelay == Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid scene interval");
        if (paused) return Action.NONE;
        if (scene != null && !scene.equals(currentScene) && playing) {
            scene = currentScene;
            delay = between(0, minDelay / 2);
            return Action.STOP;
        }
        scene = currentScene;
        delay = Math.min(delay, maxDelay);
        if (!playing && delay-- <= 0) {
            delay = Integer.MAX_VALUE;
            return Action.PLAY;
        }
        return Action.NONE;
    }
    /** Notify only on audible completion, once, before the next tick. */
    public void finished(int minDelay, int maxDelay) {
        if (minDelay < 0 || maxDelay < minDelay || maxDelay == Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid scene interval");
        delay = Math.min(delay, between(minDelay, maxDelay));
    }
    private int between(int min, int max) { return min + random.nextInt(max - min + 1); }
}
