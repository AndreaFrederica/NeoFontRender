package neofontrender.addons.mixin;

import net.minecraft.server.MinecraftServer;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Per-chunk spawn preparation counter. This mixin is optional and may be disabled by
 * {@link neofontrender.addons.compat.ModCompatRegistry} when another mod replaces
 * {@code MinecraftServer.initialWorldChunkLoad()} (e.g. Battle Towers Fixes).
 */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServerSpawnProgressChunk {
    @Inject(method = "initialWorldChunkLoad", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;provideChunk(II)Lnet/minecraft/world/chunk/Chunk;",
            shift = At.Shift.AFTER), require = 0)
    private void nfrUi$countPreparedSpawnChunk(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.recordExactSpawnChunk(
                (MinecraftServer) (Object) this);
    }
}
