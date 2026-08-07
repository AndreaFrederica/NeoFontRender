package neofontrender.addons.compat;

import java.util.Collections;
import java.util.List;

/**
 * Battle Towers Fixes overwrites {@code MinecraftServer.initialWorldChunkLoad()} with an empty
 * body, which removes the {@code provideChunk} invocation our exact spawn-chunk counter anchors
 * to. Disable that single mixin and fall back to the chunk-event based counter.
 */
public final class BTFixesCompat implements ModCompat {
    private static final String DISABLED_MIXIN =
            "neofontrender.addons.mixin.MixinMinecraftServerSpawnProgressChunk";

    @Override
    public String id() {
        return "btfixes";
    }

    @Override
    public String displayName() {
        return "Battle Towers Fixes";
    }

    @Override
    public boolean isActive() {
        return ClassPresenceChecker.isPresent("mod.acgaming.btfixes.BTFixes");
    }

    @Override
    public List<CompatImpact> impacts() {
        return Collections.singletonList(new CompatImpact(
                CompatImpact.KIND_DISABLED_MIXIN,
                DISABLED_MIXIN,
                "neofontrender_ui_enhancements.compat.btfixes.disabled_spawn_chunk_mixin"));
    }

    @Override
    public boolean shouldApplyMixin(String mixinClassName) {
        return !DISABLED_MIXIN.equals(mixinClassName);
    }
}
