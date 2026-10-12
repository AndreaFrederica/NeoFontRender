package neofontrender.addons.camera;

import neofontrender.addons.api.camera.CameraShaderCompatibility;

import java.lang.reflect.Field;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Optional OptiFine/legacy ShadersMod resolution bridge. */
final class ShaderCameraCompat {
    private static final Logger LOGGER = LogManager.getLogger("UIE Shader Camera Compat");
    private ShaderCameraCompat() {}

    static CameraShaderCompatibility type() {
        if (present("net.optifine.shaders.Shaders")) return CameraShaderCompatibility.OPTIFINE;
        if (present("shadersmod.client.Shaders")) return CameraShaderCompatibility.SHADERS_MOD;
        return CameraShaderCompatibility.NONE;
    }

    static float resolutionMultiplier() {
        CameraShaderCompatibility type = type();
        if (type == CameraShaderCompatibility.NONE) return 1.0F;
        String name = type == CameraShaderCompatibility.OPTIFINE
                ? "net.optifine.shaders.Shaders" : "shadersmod.client.Shaders";
        try {
            Class<?> shaders = Class.forName(name, false, ShaderCameraCompat.class.getClassLoader());
            Field loaded = shaders.getField("shaderPackLoaded");
            if (!loaded.getBoolean(null)) return 1.0F;
            return Math.max(0.01F, shaders.getField("configRenderResMul").getFloat(null));
        } catch (ReflectiveOperationException error) {
            LOGGER.error("Failed to read {} camera resolution multiplier", type, error);
            throw new IllegalStateException("Failed to read " + type
                    + " camera resolution multiplier", error);
        } catch (RuntimeException error) {
            LOGGER.error("Failed to read {} camera resolution multiplier", type, error);
            throw error;
        } catch (LinkageError error) {
            LOGGER.error("Failed to read {} camera resolution multiplier", type, error);
            throw error;
        }
    }

    private static boolean present(String name) {
        ClassLoader loader = ShaderCameraCompat.class.getClassLoader();
        return loader != null && loader.getResource(name.replace('.', '/') + ".class") != null;
    }
}
