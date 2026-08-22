package neofontrender.addons.mixin;

import net.minecraft.server.MinecraftServer;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts successful spawn-chunk preparations without depending on a local-variable slot.
 *
 * <p>BugTorch's early mixin {@code MixinMinecraftServer} merges {@code initialWorldChunkLoad}
 * (SRG {@code func_71222_d}) at priority 1000. Mixin only permits injecting into a method that
 * another mixin already merged when our priority is strictly higher, so this mixin runs at
 * priority 2000. The {@code loadChunk} counting injection stays best-effort ({@code require = 0}):
 * if BugTorch rewrites the spawn-chunk loop and the call is gone, the exact counter is simply
 * not observed and the renderer falls back to {@code ChunkEvent.Load} bookkeeping.
 */
@Mixin(value = MinecraftServer.class, priority = 2000)
public abstract class MixinMinecraftServerSpawnProgress {
    @Inject(method = "initialWorldChunkLoad", at = @At("HEAD"))
    private void nfrUi$beginSpawnPreparation(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.beginExactSpawnPreparation(
                (MinecraftServer) (Object) this);
    }

    // 1.7.10 adaptation: initialWorldChunkLoad feeds chunks through loadChunk,
    // whereas the 1.12.2 original calls provideChunk.
    @Inject(method = "initialWorldChunkLoad", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;loadChunk(II)Lnet/minecraft/world/chunk/Chunk;",
            shift = At.Shift.AFTER), require = 0)
    private void nfrUi$countPreparedSpawnChunk(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.recordExactSpawnChunk(
                (MinecraftServer) (Object) this);
    }

    @Inject(method = "initialWorldChunkLoad", at = @At("RETURN"))
    private void nfrUi$finishSpawnPreparation(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.finishExactSpawnPreparation(
                (MinecraftServer) (Object) this);
    }
}
