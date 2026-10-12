package neofontrender.addons.audio;

import neofontrender.audio.Playlist;
import paulscode.sound.SoundSystem;
import neofontrender.addons.build.UiBuildFeatures;
import java.nio.file.Path;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/** Client-thread owned playback handle. UI operations are serialized through a command queue. */
public final class MusicPlayer implements AutoCloseable {
    public enum State { STOPPED, LOADING, PLAYING, PAUSED, FINISHED, FAILED, CLOSED }
    private interface Command { void run(); }
    private interface Event { void apply(); }
    private final Playlist<Path> queue = new Playlist<>(new Random());
    private final List<Consumer<State>> listeners = new ArrayList<>();
    private final Queue<Command> commands = new ConcurrentLinkedQueue<>();
    private final Queue<Event> events = new ConcurrentLinkedQueue<>();
    private SoundSystem system;
    private String source, url;
    private LavaStreamingCodec.Request request;
    private State state = State.STOPPED;
    private boolean userPaused, autoAdvance = true;
    private float volume = 1, pitch = 1;
    private long position, started, generation;
    private Long pendingSeek;
    private long seekRequestedAt;
    private boolean rebuilding, startPending;
    private long startRequestedAt;
    private boolean openedEventQueued, eofEventQueued;
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
    private void command(Command command) { commands.add(command); }

    public void play(Path file) { command(() -> { queue.replace(Collections.singletonList(file)); queue.select(0); beginPlay(0); }); }
    public void select(int index) { command(() -> { queue.select(index); beginPlay(0); }); }
    public void next() { command(() -> { if (queue.next(false) != null) beginPlay(0); else stopNow(); }); }
    public void previous() { command(() -> { if (queue.previous() != null) beginPlay(0); }); }
    public void volume(float value) {
        if (!Float.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Volume 0..1");
        volume = value; command(() -> { if (system != null && source != null) system.setVolume(source, value); });
    }
    public void pitch(float value) {
        if (!Float.isFinite(value) || value < .5f || value > 2) throw new IllegalArgumentException("Pitch .5..2");
        pitch = value; command(() -> { if (system != null && source != null) system.setPitch(source, value); });
    }
    public void pause(boolean value) { command(() -> { userPaused = value; applyPause(); }); }
    public void seek(long millis) {
        command(() -> {
            if (request == null || (state != State.PLAYING && state != State.PAUSED && state != State.LOADING)
                    || millis < 0 || millis > durationMillis()) return;
            if (UiBuildFeatures.DIAGNOSTIC_LOGS)
                AudioModule.LOG.info("Seek requested: file={}, target={}ms, state={}, source={}", queue.current(), millis, state, source);
            pendingSeek = millis; seekRequestedAt = System.nanoTime(); position = millis;
        });
    }
    private void applyPause() {
        if (system == null || source == null || request == null || !request.opened) return;
        if (userPaused) { system.pause(source); state(State.PAUSED); }
        else { system.play(source); state(State.PLAYING); }
    }

    private void beginPlay(long offset) {
        if (state == State.CLOSED) throw new IllegalStateException("Player closed");
        pendingSeek = null; rebuilding = true; generation++;
        release(); position = offset; error = ""; openedEventQueued = eofEventQueued = false;
        SoundSystem live = GameAudioBackend.readySystem();
        if (live == null) { startPending = true; startRequestedAt = System.nanoTime(); state(State.LOADING); return; }
        system = live; startPending = false;
        try {
            source = "uie_music_" + UUID.randomUUID();
            URL location = new URL("file", "", "/" + source + ".nfraudio");
            url = location.toExternalForm();
            request = new LavaStreamingCodec.Request(queue.current(), offset, generation);
            LavaStreamingCodec.REQUESTS.put(url, request);
            system.newStreamingSource(true, source, location, source + ".nfraudio", false, 0, 0, 0, 0, 0);
            system.setVolume(source, volume); system.setPitch(source, pitch);
            try { system.play(source); } catch (RuntimeException e) { AudioModule.LOG.warn("Initial play queued before source registration: {}", source, e); }
            started = System.nanoTime(); state(State.LOADING);
        } catch (Exception e) {
            AudioModule.LOG.error("Cannot start audio: file={}, offset={}, source={}", queue.current(), offset, source, e);
            error = e.toString(); release(); state(State.FAILED);
        }
    }

    void tick() {
        if (state == State.CLOSED) return;
        drainCommands();
        if (startPending) {
            if (System.nanoTime() - startRequestedAt > 30_000_000_000L) { startPending = false; error = "Audio device unavailable"; state(State.FAILED); return; }
            beginPlay(position); return;
        }
        if (request == null || source == null) return;
        SoundSystem live = GameAudioBackend.readySystem();
        if (live == null) return;
        if (system != live) { beginPlay(position); return; }
        pollAsyncEvents();
        drainEvents();
        if (request == null || source == null) return;
        if (!request.opened) {
            if (System.nanoTime() - started > 30_000_000_000L) enqueueFailure(request, "Audio startup timeout");
            return;
        }
        if (state == State.LOADING && !userPaused) {
            try { system.play(source); } catch (RuntimeException e) {
                if (System.nanoTime() - started > 1_000_000_000L) AudioModule.LOG.warn("Retry play still failing for source {}", source, e);
            }
        }
        if (pendingSeek != null && !rebuilding && System.nanoTime() - seekRequestedAt >= 250_000_000L) {
            long target = pendingSeek; pendingSeek = null; boolean resume = !userPaused;
            beginPlay(target); userPaused = !resume; return;
        }
        if (userPaused) return;
        float playedMillis = GameAudioBackend.guard(() -> system.millisecondsPlayed(source), 0.0f);
        position = request.offset + Math.max(0L, (long) playedMillis);
    }
    private void drainCommands() { Command command; while ((command = commands.poll()) != null) try { command.run(); } catch (RuntimeException e) { AudioModule.LOG.warn("Audio command failed", e); } }
    private void pollAsyncEvents() {
        LavaStreamingCodec.Request current = request;
        if (current == null) return;
        if (!current.error.isEmpty()) enqueueFailure(current, current.error);
        if (current.opened && !openedEventQueued) {
            openedEventQueued = true; long id = current.generation;
            events.add(() -> { if (valid(id)) { rebuilding = false; if (userPaused) applyPause(); else state(State.PLAYING); } });
        }
        if (current.eof && !eofEventQueued) {
            eofEventQueued = true; long id = current.generation;
            events.add(() -> { if (valid(id) && !userPaused && !GameAudioBackend.guard(() -> system.playing(source), true)) finishCurrent(); });
        }
    }
    private void enqueueFailure(LavaStreamingCodec.Request failed, String message) {
        if (failed.errorEventQueued) return;
        failed.errorEventQueued = true; long id = failed.generation;
        events.add(() -> { if (valid(id)) { error = message; release(); state(State.FAILED); } });
    }
    private void drainEvents() { Event event; while ((event = events.poll()) != null) try { event.apply(); } catch (RuntimeException e) { AudioModule.LOG.warn("Audio event failed", e); } }
    private boolean valid(long id) { return request != null && request.generation == id && generation == id && state != State.CLOSED; }
    private void finishCurrent() { release(); state(State.FINISHED); if (autoAdvance && queue.next(true) != null) beginPlay(0); }
    private void stopNow() { generation++; startPending = false; events.clear(); release(); userPaused = false; state(State.STOPPED); }
    private void release() {
        LavaStreamingCodec.Request oldRequest = request;
        if (oldRequest != null) oldRequest.cancelled = true;
        if (system != null && source != null) {
            String closing = source; SoundSystem target = system;
            GameAudioBackend.guard(() -> { target.stop(closing); target.removeSource(closing); });
        }
        if (url != null) LavaStreamingCodec.REQUESTS.remove(url);
        source = url = null; request = null; openedEventQueued = eofEventQueued = false;
    }
    public void stop() { command(this::stopNow); }
    @Override public void close() {
        // close() is also called from the JVM shutdown hook, where no client tick may
        // remain to drain the command queue. It is therefore the lifecycle exception
        // to the normal queued-operation rule.
        if (state == State.CLOSED) return;
        commands.clear(); events.clear(); stopNow(); state(State.CLOSED); listeners.clear();
    }
}
