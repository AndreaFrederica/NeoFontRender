package neofontrender.audio;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SceneMusicPolicyTest {
    @Test void sameSceneNeverStopsTheSelectedTrack() {
        SceneMusicPolicy policy = new SceneMusicPolicy(new Random(0));
        assertEquals(SceneMusicPolicy.Action.NONE, policy.tick("GAME", 10, 20, true, false));
        assertEquals(SceneMusicPolicy.Action.NONE, policy.tick("GAME", 10, 20, true, false));
        assertEquals(SceneMusicPolicy.Action.STOP, policy.tick("NETHER", 10, 20, true, false));
    }
    @Test void pausedTimeDoesNotConsumeDelay() {
        SceneMusicPolicy policy = new SceneMusicPolicy(new Random(0));
        for (int i = 0; i < 200; i++) policy.tick("MENU", 0, 0, false, true);
        assertEquals(100, policy.remainingTicks());
        assertEquals(SceneMusicPolicy.Action.PLAY, policy.tick("MENU", 0, 0, false, false));
        policy.finished(10, 10);
        assertEquals(10, policy.remainingTicks());
    }
}
