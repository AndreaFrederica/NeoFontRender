package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldSettings;
import neofontrender.addons.loading.WorldLoadingRenderer;
import neofontrender.addons.loading.WorldLoadingSnapshotManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftWorldLoadingSnapshot {
    @Inject(method = "launchIntegratedServer", at = @At("HEAD"))
    private void nfrUi$prepareWorldSnapshot(String folderName, String worldName,
                                            WorldSettings settings, CallbackInfo ci) {
        WorldLoadingSnapshotManager.INSTANCE.prepareForWorld(folderName);
        WorldLoadingRenderer.INSTANCE.beginIntegratedWorldLaunch();
    }

    /**
     * Vanilla only polls the integrated server every 200 ms here. While our loading presentation
     * is active, use a frame-sized wait so time-based Arc3D interpolation is actually presented.
     */
    @ModifyConstant(method = "launchIntegratedServer", constant = @Constant(longValue = 200L))
    private long nfrUi$smoothIntegratedLoadingPoll(long original) {
        return WorldLoadingRenderer.INSTANCE.isIntegratedLaunchActive() ? 16L : original;
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
            at = @At("HEAD"))
    private void nfrUi$captureWorldSnapshot(WorldClient nextWorld, String loadingMessage,
                                            CallbackInfo ci) {
        if (nextWorld == null) {
            WorldLoadingSnapshotManager.INSTANCE.saveCurrentWorldAndRelease();
        }
    }
}
