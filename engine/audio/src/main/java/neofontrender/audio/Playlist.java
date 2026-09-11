package neofontrender.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Ordered queue; positions distinguish duplicate tracks. Own on one control thread. */
public final class Playlist<T> {
    public enum Mode { SEQUENTIAL, REPEAT_ALL, REPEAT_ONE, SHUFFLE }
    private final List<T> tracks = new ArrayList<>();
    private final List<Integer> shuffled = new ArrayList<>();
    private final Random random;
    private int index = -1;
    private Mode mode = Mode.SEQUENTIAL;
    public Playlist(Random random) { this.random = Objects.requireNonNull(random); }
    public List<T> tracks() { return Collections.unmodifiableList(new ArrayList<>(tracks)); }
    public int index() { return index; }
    public T current() { return index < 0 ? null : tracks.get(index); }
    public Mode mode() { return mode; }
    public void mode(Mode value) { mode = Objects.requireNonNull(value); shuffled.clear(); }
    public void replace(List<T> values) {
        for (T value : values) Objects.requireNonNull(value);
        tracks.clear(); tracks.addAll(values); index = -1; shuffled.clear();
    }
    public T select(int position) {
        if (position < 0 || position >= tracks.size()) throw new IndexOutOfBoundsException();
        index = position; shuffled.remove(Integer.valueOf(position)); return current();
    }
    public void move(int from, int to) {
        if (from < 0 || to < 0 || from >= tracks.size() || to >= tracks.size()) throw new IndexOutOfBoundsException();
        T value = tracks.remove(from); tracks.add(to, value);
        if (index == from) index = to;
        else if (from < index && to >= index) index--;
        else if (from > index && to <= index) index++;
        shuffled.clear();
    }
    public T next(boolean automatic) {
        if (tracks.isEmpty()) return null;
        if (automatic && mode == Mode.REPEAT_ONE && index >= 0) return current();
        if (mode == Mode.SHUFFLE) {
            if (shuffled.isEmpty()) {
                for (int i = 0; i < tracks.size(); i++) if (i != index) shuffled.add(i);
                if (shuffled.isEmpty()) shuffled.add(0);
                Collections.shuffle(shuffled, random);
            }
            return select(shuffled.get(0));
        }
        int next = index + 1;
        if (next >= tracks.size()) {
            if (mode == Mode.SEQUENTIAL) return null;
            next = 0;
        }
        return select(next);
    }
    public T previous() { return tracks.isEmpty() ? null : select(Math.max(0, index - 1)); }
}
