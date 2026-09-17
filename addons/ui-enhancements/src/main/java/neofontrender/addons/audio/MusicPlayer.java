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
    private Long pendingSeek;
    private long seekRequestedAt;
    private boolean rebuilding;
    /** Set when playAt() could not start because the sound engine was still coming up. */
    private boolean startPending;
    private long startRequestedAt;
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
        if (request == null || (state != State.PLAYING && state != State.PAUSED && state != State.LOADING)
                || millis < 0 || millis > durationMillis()) return;
        AudioModule.LOG.info("Seek requested: file={}, target={}ms, state={}, source={}", queue.current(), millis, state, source);
        pendingSeek = millis;
        seekRequestedAt = System.nanoTime();
        position = millis;
    }
    private void playAt(long offset) {
        if (state == State.CLOSED) throw new IllegalStateException("Player closed");
        pendingSeek = null;
        rebuilding = true;
        release(); position = offset; error = "";
        SoundSystem live = GameAudioBackend.readySystem();
        if (live == null) {
            // The sound engine is still starting up (or genuinely absent). Nothing was created yet,
            // so the player stays retryable instead of being permanently marked FAILED.
            startPending = true;
            startRequestedAt = System.nanoTime();
            return;
        }
        system = live;
        startPending = false;
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
            // SoundSystem queues source creation asynchronously. A transient
            // "source not found" must not tear down the request; the next tick
            // retries play while the codec remains registered.
            try { system.play(source); } catch (RuntimeException error) { AudioModule.LOG.warn("Initial play queued before source registration: {}", source, error); }
            started = System.nanoTime(); state(State.LOADING);
        } catch (Exception e) { AudioModule.LOG.error("Cannot start audio: file={}, offset={}, source={}", queue.current(), offset, source, e); error = e.toString(); release(); state(State.FAILED); }
    }
    void tick() {
        if (state == State.CLOSED) return;
        if (startPending) {
            // Retry the start until the sound engine finishes loading, but give up rather than
            // polling forever if it never becomes available.
            if (System.nanoTime() - startRequestedAt > 30_000_000_000L) {
                startPending = false;
                error = "Audio device unavailable";
                state(State.FAILED);
                return;
            }
            playAt(position);
            return;
        }
        if (source == null) return;
        // Debounce slider events and serialize source replacement. A new seek
        // requested while the codec is opening is kept as the latest target;
        // it is applied only after the current source is ready.
        if (pendingSeek != null && !rebuilding && System.nanoTime() - seekRequestedAt >= 250_000_000L) {
            long target = pendingSeek;
            pendingSeek = null;
            boolean resume = !userPaused;
            playAt(target);
            userPaused = !resume;
            return;
        }
        SoundSystem live = GameAudioBackend.readySystem();
        // A system that is still starting up reports null: neither adopt it nor treat it as replaced,
        // just wait for the next tick.
        if (live == null) return;
        if (system != live) { playAt(position); return; }
        if (!request.error.isEmpty()) { AudioModule.LOG.error("Audio codec failed: file={}, source={}, offset={}, error={}", queue.current(), source, request.offset, request.error); error = request.error; release(); state(State.FAILED); return; }
        if (!request.opened) {
            if (System.nanoTime() - started > 30_000_000_000L) { error = "Audio startup timeout"; release(); state(State.FAILED); }
            return;
        }
        rebuilding = false;
        if (state == State.LOADING && !userPaused) {
            try { system.play(source); } catch (RuntimeException error) {
                if (System.nanoTime() - started > 1_000_000_000L)
                    AudioModule.LOG.warn("Retry play still failing for source {}", source, error);
            }
        }
        if (state == State.LOADING) {
            if (userPaused) applyPause();
            else state(State.PLAYING);
        }
        if (userPaused) return;
        position = request.offset + Math.max(0L,
                (long) GameAudioBackend.guard(() -> system.millisecondsPlayed(source), 0));
        if (request.eof && !GameAudioBackend.guard(() -> system.playing(source), true)) {
            release(); state(State.FINISHED);
            if (autoAdvance && queue.next(true) != null) playAt(0);
        }
    }
    private void release() {
        LavaStreamingCodec.Request oldRequest = request;
        if (oldRequest != null) oldRequest.cancelled = true;
        if (system != null && source != null) {
            String closing = source;
            SoundSystem target = system;
            GameAudioBackend.guard(() -> { target.stop(closing); target.removeSource(closing); });
        }
        if (url != null) LavaStreamingCodec.REQUESTS.remove(url);
        source = url = null;
        request = null;
    }
    public void stop() { release(); userPaused = false; state(State.STOPPED); }
    @Override public void close() { if (state == State.CLOSED) return; release(); state(State.CLOSED); listeners.clear(); }
}
