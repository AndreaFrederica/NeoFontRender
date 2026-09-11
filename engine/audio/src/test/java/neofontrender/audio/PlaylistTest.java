package neofontrender.audio;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
class PlaylistTest {
    @Test void duplicateEntriesKeepTheirIdentityWhenMoved() {
        Playlist<String> queue = new Playlist<>(new Random(1));
        queue.replace(Arrays.asList("same", "different", "same"));
        queue.select(2); queue.move(0, 2);
        assertEquals(1, queue.index());
        assertEquals("same", queue.current());
    }
    @Test void manualNextEscapesRepeatOneAndSequentialStops() {
        Playlist<String> queue = new Playlist<>(new Random(1));
        queue.replace(Arrays.asList("a", "b"));
        assertEquals("a", queue.next(true));
        queue.mode(Playlist.Mode.REPEAT_ONE);
        assertEquals("a", queue.next(true));
        assertEquals("b", queue.next(false));
        queue.mode(Playlist.Mode.SEQUENTIAL);
        assertNull(queue.next(true));
    }
}
