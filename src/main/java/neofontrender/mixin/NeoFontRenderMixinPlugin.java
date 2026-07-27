package neofontrender.mixin;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.lib.tree.ClassNode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * Applies optional sign optimizations only when at least one sign option is enabled at launch.
 * Runtime checks remain in the mixin for config reloads, but this gate preserves the original
 * target bytecode entirely when both options are disabled.
 */
public final class NeoFontRenderMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("NeoFontRender Mixin Plugin");

    private boolean signMixinEnabled = true;
    private boolean signDispatcherMixinEnabled = true;

    @Override
    public void onLoad(String mixinPackage) {
        SignOptions options = readSignOptions();
        signMixinEnabled = options.anySignOptimization;
        signDispatcherMixinEnabled = options.dispatcherOptimization;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("MixinTileEntitySignRenderer")) {
            return signMixinEnabled;
        }
        if (mixinClassName.endsWith("MixinTileEntityRendererDispatcher")) {
            return signDispatcherMixinEnabled;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static SignOptions readSignOptions() {
        File config = new File(gameDirectory(), "config/neofontrender.toml");
        if (!config.isFile()) {
            // The generated defaults enable both optimizations.
            return new SignOptions(true, true);
        }
        boolean inPerformance = false;
        boolean lod = true;
        boolean frustum = true;
        boolean modelLod = false;
        boolean blockOcclusionCulling = true;
        try {
            List<String> lines = Files.readAllLines(config.toPath(), StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.split("#", 2)[0].trim();
                if (line.startsWith("[")) {
                    inPerformance = "[performance]".equals(line);
                    continue;
                }
                if (!inPerformance) {
                    continue;
                }
                String key = key(line);
                if ("signTextLodCulling".equals(key)) {
                    lod = parseBoolean(line, lod);
                } else if ("signTextFrustumCulling".equals(key)) {
                    frustum = parseBoolean(line, frustum);
                } else if ("signModelLod".equals(key)) {
                    modelLod = parseBoolean(line, modelLod);
                } else if ("signBlockOcclusionCulling".equals(key)) {
                    blockOcclusionCulling = parseBoolean(line, blockOcclusionCulling);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read sign options from " + config + "; keeping sign optimizations enabled", e);
            return new SignOptions(true, true);
        }
        return new SignOptions(lod || frustum || modelLod || blockOcclusionCulling,
                blockOcclusionCulling);
    }

    private static String key(String line) {
        int equals = line.indexOf('=');
        return equals < 0 ? "" : line.substring(0, equals).trim();
    }

    private static boolean parseBoolean(String line, boolean fallback) {
        int equals = line.indexOf('=');
        if (equals < 0) {
            return fallback;
        }
        String value = line.substring(equals + 1).trim();
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }

    private static File gameDirectory() {
        if (Launch.minecraftHome != null) {
            return Launch.minecraftHome;
        }
        LOGGER.warn("Launch.minecraftHome is unavailable during early Mixin loading; falling back to user.dir");
        try {
            return new File(System.getProperty("user.dir", "."));
        } catch (SecurityException e) {
            LOGGER.error("Unable to read user.dir; falling back to the current working directory", e);
            return new File(".");
        }
    }

    private static final class SignOptions {
        private final boolean anySignOptimization;
        private final boolean dispatcherOptimization;

        private SignOptions(boolean anySignOptimization, boolean dispatcherOptimization) {
            this.anySignOptimization = anySignOptimization;
            this.dispatcherOptimization = dispatcherOptimization;
        }
    }
}
