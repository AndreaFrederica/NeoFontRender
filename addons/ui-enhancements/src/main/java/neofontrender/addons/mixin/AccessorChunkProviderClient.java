package neofontrender.addons.mixin;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.LongHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkProviderClient.class)
public interface AccessorChunkProviderClient {
    /**
     * 1.7.10 stores client chunks in a LongHashMap named chunkMapping instead of 1.12's fastutil
     * loadedChunks map; callers size it through getNumHashElements().
     */
    @Accessor("chunkMapping")
    LongHashMap nfrUi$getLoadedChunks();
}
