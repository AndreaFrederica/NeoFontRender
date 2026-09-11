package neofontrender.addons.audio;

import neofontrender.audio.Playlist;
import paulscode.sound.SoundSystem;
import java.nio.file.Path;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/** Client-thread owned playback handle. Close releases only this player's sources. */
public final class MusicPlayer implements AutoCloseable {
    public enum State { STOPPED, LOADING, PLAYING, PAUSED, FINISHED, FAILED, CLOSED }
    private final Playlist<Path> queue = new Playlist<>(new Random());
    private final List<Consumer<State>> listeners = new ArrayList<>();
    private SoundSystem system;
    private String source, url;
    private LavaStreamingCodec.Request request;
    private State state = State.STOPPED;
    private boolean userPaused, autoAdvance = true;
    private float volume = 1, pitch = 1;
    private long position, started;
    private String error = "";
    public State state() { return state; }
    public String error() { return error; }
    public Playlist<Path> playlist() { return queue; }
    public long positionMillis() { return position; }
    public long durationMillis() { return request == null ? 0 : request.duration; }
    public float volume() { return volume; }
    public void autoAdvance(boolean value) { autoAdvance = value; }
    public AutoCloseable listen(Consumer<State> listener) { listeners.add(listener); return () -> listeners.remove(listener); }
    private void state(State next) {
        if (state == next) return;
        state = next;
        for (Consumer<State> listener : new ArrayList<>(listeners)) {
            try { listener.accept(next); } catch (RuntimeException e) { AudioModule.LOG.warn("Audio listener failed", e); }
        }
    }
    public void play(Path file) { queue.replace(Collections.singletonList(file)); queue.select(0); playAt(0); }
    public void select(int index) { queue.select(index); playAt(0); }
    public void next() { if (queue.next(false) != null) playAt(0); else stop(); }
    public void previous() { if (queue.previous() != null) playAt(0); }
    public void volume(float value) {
        if (!Float.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Volume 0..1");
        volume = value; if (system != null && source != null) system.setVolume(source, value);
    }
    public void pitch(float value) {
        if (!Float.isFinite(value) || value < .5f || value > 2) throw new IllegalArgumentException("Pitch .5..2");
        pitch = value; if (system != null && source != null) system.setPitch(source, value);
    }
    public void pause(boolean value) { userPaused = value; applyPause(); }
    /** Music follows vanilla behavior: it keeps playing while a GUI or the pause menu is open. */
    private void applyPause() {
        if (system == null || source == null || request == null || !request.opened) return;
        if (userPaused) { system.pause(source); state(State.PAUSED); }
        else { system.play(source); state(State.PLAYING); }
    }
    public void seek(long millis) {
        if (request == null || millis < 0 || millis > durationMillis()) throw new IllegalArgumentException("Invalid position");
        boolean resume = !userPaused;
        playAt(millis);
        userPaused = !resume;
    }
    private void playAt(long offset) {
        if (state == State.CLOSED) throw new IllegalStateException("Player closed");
        release(); position = offset; error = "";
        system = GameAudioBackend.system();
        if (system == null) { state(State.FAILED); error = "Audio device unavailable"; return; }
        try {
            source = "uie_music_" + UUID.randomUUID();
            URL location = new URL("file", "", "/" + source + ".nfraudio");
            url = location.toExternalForm();
            request = new LavaStreamingCodec.Request(queue.current(), offset);
            LavaStreamingCodec.REQUESTS.put(url, request);
            system.newStreamingSource(true, source, location, source + ".nfraudio", false, 0, 0, 0, 0, 0);
            system.setVolume(source, volume); system.setPitch(source, pitch);
            // Commands are queued in order. play starts preLoad/codec initialization;
            // waiting for request.opened before this call would deadlock startup.
            system.play(source); started = System.nanoTime(); state(State.LOADING);
        } catch (Exception e) { error = e.toString(); release(); state(State.FAILED); }
    }
    void tick() {
        if (source == null || state == State.CLOSED) return;
        SoundSystem live = GameAudioBackend.system();
        if (system != live) { playAt(position); return; }
        if (!request.error.isEmpty()) { error = request.error; release(); state(State.FAILED); return; }
        if (!request.opened) {
            if (System.nanoTime() - started > 30_000_000_000L) { error = "Audio startup timeout"; release(); state(State.FAILED); }
            return;
        }
        if (state == State.LOADING) {
            if (userPaused) applyPause();
            else state(State.PLAYING);
        }
        if (userPaused) return;
        position = request.offset + Math.max(0, (long) system.millisecondsPlayed(source));
        if (request.eof && !system.playing(source)) {
            release(); state(State.FINISHED);
            if (autoAdvance && queue.next(true) != null) playAt(0);
        }
    }
    private void release() {
        if (system != null && source != null) {
            try { system.stop(source); system.removeSource(source); }
            catch (Exception e) { AudioModule.LOG.debug("Audio device already gone while releasing {}", source, e); }
        }
        if (url != null) LavaStreamingCodec.REQUESTS.remove(url);
        source = url = null;
    }
    public void stop() { release(); userPaused = false; state(State.STOPPED); }
    @Override public void close() { if (state == State.CLOSED) return; release(); state(State.CLOSED); listeners.clear(); }
}
