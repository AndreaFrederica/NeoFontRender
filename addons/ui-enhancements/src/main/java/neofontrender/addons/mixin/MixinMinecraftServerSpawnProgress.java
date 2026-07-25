package neofontrender.addons.mixin;

import net.minecraft.server.MinecraftServer;
import neofontrender.addons.loading.WorldLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts successful spawn-chunk preparations without depending on a local-variable slot. */
@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServerSpawnProgress {
    @Inject(method = "initialWorldChunkLoad", at = @At("HEAD"))
    private void nfrUi$beginSpawnPreparation(CallbackInfo ci) {
        WorldLoadingRenderer.INSTANCE.beginExactSpawnPreparation(
                (MinecraftServer) (Object) this);
    }

    @Inject(method = "initialWorldChunkLoad", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/gen/ChunkProviderServer;provideChunk(II)Lnet/minecraft/world/chunk/Chunk;",
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
