package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CosmicRasterPixelsTest {
    @Test
    void copiesOnlyBaseLayerAndPreservesOptifineLayers() {
        ByteBuffer source = ByteBuffer.allocate(4 * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        source.putInt(0x11223344);
        source.putInt(0x55667788);
        source.putInt(0x99AABBCC);
        source.putInt(0xDDEEFF00);
        source.flip();

        int sentinel = 0xFF7F7FFF;
        int[] target = new int[12];
        Arrays.fill(target, sentinel);

        CosmicRasterPixels.copyBaseLayer(source, 4, target);

        assertArrayEquals(new int[] {
                0x11223344, 0x55667788, 0x99AABBCC, 0xDDEEFF00
        }, Arrays.copyOf(target, 4));
        for (int index = 4; index < target.length; index++) {
            assertEquals(sentinel, target[index]);
        }
        assertEquals(0, source.remaining());
    }

    @Test
    void rejectsTextureStorageSmallerThanBaseLayer() {
        ByteBuffer source = ByteBuffer.allocate(4 * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        assertThrows(IllegalStateException.class,
                () -> CosmicRasterPixels.copyBaseLayer(source, 4, new int[3]));
    }

    @Test
    void rejectsTruncatedNativePayload() {
        ByteBuffer source = ByteBuffer.allocate(3 * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        assertThrows(IllegalStateException.class,
                () -> CosmicRasterPixels.copyBaseLayer(source, 4, new int[4]));
    }
}
