package neofontrender.addons.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paulscode.sound.SoundBuffer;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LavaStreamingCodecTest {
    @TempDir Path directory;

    @Test void openAlReceivesNativePcmThroughEndOfTrack() throws Exception {
        int samples = 48000 * 4;
        ByteBuffer wav = ByteBuffer.allocate(44 + samples * 4).order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes()).putInt(36 + samples * 4).put("WAVEfmt ".getBytes());
        wav.putInt(16).putShort((short) 1).putShort((short) 2).putInt(48000);
        wav.putInt(192000).putShort((short) 4).putShort((short) 16);
        wav.put("data".getBytes()).putInt(samples * 4);
        for (int i = 0; i < samples; i++) {
            short value = (short) (Math.sin(i * 2 * Math.PI * 440 / 48000) * 1000);
            wav.putShort(value).putShort(value);
        }
        Path file = Files.write(directory.resolve("tone.wav"), wav.array());
        URL url = new URL("file:/codec-test.nfraudio");
        LavaStreamingCodec.Request request = new LavaStreamingCodec.Request(file, 0);
        LavaStreamingCodec.REQUESTS.put(url.toExternalForm(), request);
        LavaStreamingCodec codec = new LavaStreamingCodec();
        try {
            codec.reverseByteOrder(true); // exactly what SourceLWJGLOpenAL requests
            assertTrue(codec.initialize(url));
            assertTrue(request.opened);
            assertEquals(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN, codec.getAudioFormat().isBigEndian());
            long total = 0;
            for (SoundBuffer block; (block = codec.read()) != null;) {
                if (total == 0) assertTrue(block.audioData.length >= 16384);
                ByteBuffer pcm = ByteBuffer.wrap(block.audioData).order(ByteOrder.nativeOrder());
                while (pcm.remaining() >= 2) assertTrue(Math.abs((int) pcm.getShort()) <= 1010,
                        "Byte-swapped samples would turn this quiet tone into loud noise");
                total += block.audioData.length;
            }
            assertEquals(samples * 4L, total);
            assertTrue(request.eof);
            assertEquals("", request.error);
        } finally {
            codec.cleanup();
            LavaStreamingCodec.REQUESTS.remove(url.toExternalForm());
        }
    }
}
