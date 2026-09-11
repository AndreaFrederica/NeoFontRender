package neofontrender.audio;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.*;

/** Pull decoder for the game's streaming codec thread. Never read from a UI thread. */
public final class LavaPcmStream implements AutoCloseable {
    private final DefaultAudioPlayerManager manager;
    private final AudioPlayer player;
    private volatile boolean closed;
    private volatile Throwable failure;
    private long duration;
    public LavaPcmStream(Path file, long startMillis) throws IOException {
        manager = new DefaultAudioPlayerManager();
        manager.setUseSeekGhosting(false);
        manager.setFrameBufferDuration(200);
        manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.DISCORD_PCM_S16_LE);
        manager.registerSourceManager(new LocalAudioSourceManager());
        player = manager.createPlayer();
        player.addListener(new AudioEventAdapter() {
            @Override public void onTrackException(AudioPlayer p, AudioTrack t, FriendlyException e) { failure = e; }
            @Override public void onTrackStuck(AudioPlayer p, AudioTrack t, long ms) { failure = new IOException("Decoder stalled"); }
        });
        CompletableFuture<AudioTrack> loaded = new CompletableFuture<>();
        manager.loadItem(file.toAbsolutePath().toString(), new AudioLoadResultHandler() {
            public void trackLoaded(AudioTrack track) { loaded.complete(track); }
            public void playlistLoaded(AudioPlaylist list) { loaded.completeExceptionally(new IOException("Expected an audio file")); }
            public void noMatches() { loaded.completeExceptionally(new IOException("Unsupported audio: " + file)); }
            public void loadFailed(FriendlyException error) { loaded.completeExceptionally(error); }
        });
        try {
            AudioTrack track = loaded.get(15, TimeUnit.SECONDS);
            duration = track.getDuration();
            if (startMillis > 0) {
                if (!track.isSeekable()) throw new IOException("Track is not seekable");
                track.setPosition(Math.min(startMillis, duration));
            }
            player.playTrack(track);
        } catch (Exception error) {
            close();
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IOException("Cannot open " + file, error);
        }
    }
    public long durationMillis() { return duration; }
    public byte[] read() throws IOException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!closed) {
            if (failure != null) throw new IOException("Audio decode failed", failure);
            try {
                AudioFrame frame = player.provide(100, TimeUnit.MILLISECONDS);
                if (failure != null) throw new IOException("Audio decode failed", failure);
                if (frame != null) return frame.getData().clone();
                if (player.getPlayingTrack() == null) return null;
                if (System.nanoTime() >= deadline) throw new IOException("Audio decode timeout");
            } catch (TimeoutException ignored) {
                if (System.nanoTime() >= deadline) throw new IOException("Audio decode timeout");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt(); throw new IOException(error);
            }
        }
        return null;
    }
    @Override public void close() {
        if (closed) return;
        closed = true;
        player.destroy(); manager.shutdown();
    }
}
