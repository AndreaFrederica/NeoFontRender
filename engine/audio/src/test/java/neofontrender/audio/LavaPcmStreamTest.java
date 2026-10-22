package neofontrender.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class LavaPcmStreamTest {
    @TempDir Path directory;

    private Path tone() throws Exception {
        int bytes = 48000 * 2 * 2;
        ByteBuffer wav = ByteBuffer.allocate(44 + bytes).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes()).putInt(36 + bytes).put("WAVEfmt ".getBytes());
        wav.putInt(16).putShort((short) 1).putShort((short) 2).putInt(48000);
        wav.putInt(192000).putShort((short) 4).putShort((short) 16);
        wav.put("data".getBytes()).putInt(bytes);
        for (int i = 0; i < 48000; i++) {
            short sample = (short) (Math.sin(i * 2 * Math.PI * 440 / 48000) * 10000);
            wav.putShort(sample).putShort(sample);
        }
        return Files.write(directory.resolve("tone.wav"), wav.array());
    }

    private static long drain(LavaPcmStream stream) throws IOException {
        long total = 0;
        for (byte[] frame; (frame = stream.read()) != null; ) {
            assertEquals(3840, frame.length);
            total += frame.length;
        }
        return total;
    }

    @Test void decodeWavProducesOneSecondOfPcm() throws Exception {
        try (LavaPcmStream stream = new LavaPcmStream(tone(), 0)) {
            assertEquals(1000, stream.durationMillis());
            long pcm = drain(stream);
            // 48 kHz stereo S16 = 192000 bytes/s; lossless WAV must be within one frame.
            assertTrue(Math.abs(pcm - 192000) <= 3840, "PCM bytes: " + pcm);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"mp3", "ogg", "flac"})
    void decodeCompressedLocalFiles(String extension) throws Exception {
        Path encoded = directory.resolve("tone." + extension);
        Process conversion = new ProcessBuilder("ffmpeg", "-v", "error", "-y", "-i",
                tone().toString(), encoded.toString()).redirectErrorStream(true).start();
        assertTrue(conversion.waitFor(20, TimeUnit.SECONDS));
        assertEquals(0, conversion.exitValue());
        try (LavaPcmStream stream = new LavaPcmStream(encoded, 0)) {
            assertTrue(drain(stream) >= 45L * 3840, "Expected about one second of PCM");
        }
    }

    @Test void seekStartsFromOffsetAndMissingFileFails() throws Exception {
        Path file = tone();
        try (LavaPcmStream stream = new LavaPcmStream(file, 500)) {
            assertEquals(1000, stream.durationMillis());
            long pcm = drain(stream);
            assertTrue(pcm >= 192000 / 2 - 3840 && pcm <= 192000 / 2 + 3840,
                    "Half a second expected, got bytes: " + pcm);
        }
        assertThrows(IOException.class, () -> new LavaPcmStream(directory.resolve("missing.wav"), 0));
    }

    @Test void closeIsIdempotentAndStopsReading() throws Exception {
        LavaPcmStream stream = new LavaPcmStream(tone(), 0);
        assertNotNull(stream.read());
        stream.close();
        stream.close();
        assertNull(stream.read());
    }
}
