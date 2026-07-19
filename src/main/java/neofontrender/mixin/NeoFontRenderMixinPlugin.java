package neofontrender.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

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
        boolean batching = true;
        boolean frustum = true;
        boolean modelLod = false;
        boolean crossTileBatching = false;
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
                } else if ("signTextBatching".equals(key)) {
                    batching = parseBoolean(line, batching);
                } else if ("signTextFrustumCulling".equals(key)) {
                    frustum = parseBoolean(line, frustum);
                } else if ("signModelLod".equals(key)) {
                    modelLod = parseBoolean(line, modelLod);
                } else if ("signCrossTileBatching".equals(key)) {
                    crossTileBatching = parseBoolean(line, crossTileBatching);
                } else if ("signBlockOcclusionCulling".equals(key)) {
                    blockOcclusionCulling = parseBoolean(line, blockOcclusionCulling);
                }
            }
        } catch (Throwable ignored) {
            // Keep the default-on behavior if the file cannot be read during early Mixin loading.
            return new SignOptions(true, true);
        }
        return new SignOptions(lod || batching || frustum || modelLod || crossTileBatching
                || blockOcclusionCulling, crossTileBatching || blockOcclusionCulling);
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
        try {
            Class<?> launch = Class.forName("net.minecraft.launchwrapper.Launch");
            Object home = launch.getField("minecraftHome").get(null);
            if (home instanceof File) {
                return (File) home;
            }
        } catch (Throwable ignored) {
        }
        return new File(System.getProperty("user.dir", "."));
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
