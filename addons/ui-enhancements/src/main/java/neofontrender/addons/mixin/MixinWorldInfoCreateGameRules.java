package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import neofontrender.addons.worldcreation.CreateWorldDifficultyState;
import neofontrender.addons.worldcreation.CreateWorldGameRulesState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Applies the game rules chosen on the create-world screen. The
 * {@code WorldInfo(WorldSettings, String)} constructor only runs when a brand-new save is
 * created (existing saves load via the NBT constructor), and its {@code gameRules} field is
 * initialized at declaration, so the vanilla defaults are ready to be overridden here.
 */
@Mixin(WorldInfo.class)
public abstract class MixinWorldInfoCreateGameRules {
    @Inject(method = "<init>(Lnet/minecraft/world/WorldSettings;Ljava/lang/String;)V",
            at = @At("RETURN"))
    private void nfrUi$applyCreateWorldGameRules(WorldSettings settings, String name, CallbackInfo ci) {
        EnumDifficulty difficulty = CreateWorldDifficultyState.consumePending();
        if (difficulty != null) nfrUi$applyDifficulty(difficulty);
        Map<String, String> pending = CreateWorldGameRulesState.consumePending();
        if (pending == null || pending.isEmpty()) return;
        GameRules rules = ((WorldInfo) (Object) this).getGameRulesInstance();
        for (Map.Entry<String, String> entry : pending.entrySet()) {
            if (rules.hasRule(entry.getKey())) {
                rules.setOrCreateGameRule(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 1.7.10 has no per-level difficulty: single-player difficulty lives in options.txt and the
     * integrated server reads it back while loading the world, so the create-world choice updates
     * that value (and persists it) instead of the freshly built {@code WorldInfo}.
     */
    private static void nfrUi$applyDifficulty(EnumDifficulty difficulty) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.gameSettings.difficulty = difficulty;
        minecraft.gameSettings.saveOptions();
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.isSinglePlayer()) server.func_147139_a(difficulty);
    }
}
