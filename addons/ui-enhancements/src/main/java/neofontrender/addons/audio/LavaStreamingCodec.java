package neofontrender.addons.audio;

import neofontrender.audio.LavaPcmStream;
import paulscode.sound.ICodec;
import paulscode.sound.SoundBuffer;
import javax.sound.sampled.AudioFormat;
import java.net.URL;
import java.nio.file.Path;
import java.nio.ByteOrder;
import java.io.ByteArrayOutputStream;
import paulscode.sound.SoundSystemConfig;
import java.util.concurrent.ConcurrentHashMap;

/** Dedicated extension; never replaces Minecraft's ogg/wav codecs. */
public final class LavaStreamingCodec implements ICodec {
    static final ConcurrentHashMap<String, Request> REQUESTS = new ConcurrentHashMap<>();
    static final class Request {
        final Path file;
        final long offset;
        volatile boolean opened, eof;
        volatile boolean cancelled;
        volatile long duration;
        volatile String error = "";
        Request(Path file, long offset) { this.file = file; this.offset = offset; }
    }
    private LavaPcmStream decoder;
    private Request request;
    private boolean ended, nativeOrder;
    // Paulscode OpenAL requests native-order samples. Lava already produces LE.
    public void reverseByteOrder(boolean value) { nativeOrder = value; }
    public boolean initialize(URL url) {
        cleanup(); ended = false;
        request = REQUESTS.get(url.toExternalForm());
        if (request == null) { ended = true; return false; }
        try {
            decoder = new LavaPcmStream(request.file, request.offset);
            request.duration = decoder.durationMillis(); request.opened = true;
            return true;
        } catch (Exception e) { if (!request.cancelled) AudioModule.LOG.error("Codec initialization failed for {}", request.file, e); request.error = e.toString(); ended = true; return false; }
    }
    public boolean initialized() { return decoder != null; }
    public SoundBuffer read() {
        if (ended || decoder == null) return null;
        try {
            // A Lava frame is only 20 ms. Supply normal Paulscode-sized buffers
            // so its streaming poll loop has enough audio queued between ticks.
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int target = Math.max(3840, SoundSystemConfig.getStreamingBufferSize());
            while (buffer.size() < target) {
                byte[] frame = decoder.read();
                if (frame == null) { ended = true; request.eof = true; break; }
                buffer.write(frame, 0, frame.length);
            }
            if (buffer.size() == 0) return null;
            byte[] pcm = buffer.toByteArray();
            if (bigEndian()) for (int i = 0; i < pcm.length; i += 2) {
                byte b = pcm[i]; pcm[i] = pcm[i + 1]; pcm[i + 1] = b;
            }
            return new SoundBuffer(pcm, getAudioFormat());
        } catch (Exception error) {
            // Replacing/seeking a track intentionally interrupts the decoder.
            // Do not surface that shutdown interruption as a playback failure.
            if (!request.cancelled && !interrupted(error)) { AudioModule.LOG.error("Codec read failed for {}", request.file, error); request.error = error.toString(); }
            ended = true; return null;
        }
    }
    private static boolean interrupted(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause())
            if (current instanceof InterruptedException) return true;
        return false;
    }
    public SoundBuffer readAll() { throw new UnsupportedOperationException("Music requires streaming"); }
    public boolean endOfStream() { return ended; }
    public void cleanup() { if (decoder != null) decoder.close(); decoder = null; ended = true; }
    private boolean bigEndian() { return nativeOrder && ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN; }
    public AudioFormat getAudioFormat() { return new AudioFormat(48000, 16, 2, true, bigEndian()); }
}
