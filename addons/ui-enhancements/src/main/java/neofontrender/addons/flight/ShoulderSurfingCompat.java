package neofontrender.addons.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Optional, linkage-free bridge for Shoulder Surfing 1.12.2. */
final class ShoulderSurfingCompat {
    private static final Logger LOGGER = LogManager.getLogger("UIE Shoulder Surfing Compat");
    private static boolean resolved;
    private static Method getInstance;
    private static Method isActive;
    private static Method getRendererInstance;
    private static Method getPlayerReach;
    private static Field rendererProjected;
    private static Method translationX;
    private static Method translationY;
    private static Method traceBlocksAndEntities;
    private static boolean adaptiveCallbackRegistered;

    private ShoulderSurfingCompat() {}

    static boolean isActive() {
        if (!Loader.isModLoaded("shouldersurfing")) return false;
        resolve();
        try {
            Object instance = getInstance.invoke(null);
            return Boolean.TRUE.equals(isActive.invoke(instance));
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("query active camera state", error);
        }
    }

    /** Registers UIE's exact-ID backports with Shoulder Surfing's public adaptive-item API. */
    static synchronized void registerAdaptiveItems() {
        if (adaptiveCallbackRegistered || !Loader.isModLoaded("shouldersurfing")) return;
        try {
            ClassLoader loader = ShoulderSurfingCompat.class.getClassLoader();
            Class<?> callbackType = Class.forName(
                    "com.teamderpy.shouldersurfing.api.callback.IAdaptiveItemCallback", false, loader);
            Class<?> registrarType = Class.forName(
                    "com.teamderpy.shouldersurfing.plugin.ShoulderSurfingRegistrar", false, loader);
            Object callback = Proxy.newProxyInstance(callbackType.getClassLoader(),
                    new Class<?>[]{callbackType}, (proxy, method, args) -> {
                        if ("isHoldingAdaptiveItem".equals(method.getName()) && args != null
                                && args.length == 2 && args[1] instanceof EntityLivingBase) {
                            return isUsingBackportItem((EntityLivingBase) args[1]);
                        }
                        if ("toString".equals(method.getName())) return "UIE backport adaptive items";
                        if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                        if ("equals".equals(method.getName())) {
                            return args != null && args.length == 1 && proxy == args[0];
                        }
                        return false;
                    });
            Object registrar = registrarType.getMethod("getInstance").invoke(null);
            registrarType.getMethod("registerAdaptiveItemCallback", callbackType)
                    .invoke(registrar, callback);
            adaptiveCallbackRegistered = true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            throw compatibilityFailure("register adaptive-item callback", error);
        }
    }

    /** Uses the same shoulder-camera ray that Shoulder Surfing projects its dynamic crosshair onto. */
    static Entity crosshairTarget(float partialTicks, Entity fallback) {
        if (!ShoulderSurfingFixConfig.enabled()) return fallback;
        RayTraceResult result = usesCameraInteraction()
                ? cameraRayTrace(partialTicks) : playerRayTrace(partialTicks);
        return result == null ? fallback : result.entityHit;
    }

    /** Keeps vanilla block/entity picking on the same player-origin ray as the projected crosshair. */
    static void synchronizeMouseOver(float partialTicks) {
        RayTraceResult result = usesCameraInteraction()
                ? cameraRayTrace(partialTicks) : playerRayTrace(partialTicks);
        if (result == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.objectMouseOver = result;
        minecraft.pointedEntity = result.typeOfHit == RayTraceResult.Type.ENTITY
                ? result.entityHit : null;
    }

    private static RayTraceResult cameraRayTrace(float partialTicks) {
        return trace(partialTicks, true);
    }

    private static RayTraceResult playerRayTrace(float partialTicks) {
        return trace(partialTicks, false);
    }

    private static RayTraceResult trace(float partialTicks, boolean shoulderCamera) {
        if (!isActive()) return null;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getRenderViewEntity() == null || minecraft.playerController == null) return null;
        try {
            Object renderer = getRendererInstance.invoke(null);
            double reach = ((Number) getPlayerReach.invoke(renderer)).doubleValue();
            Object result = traceBlocksAndEntities.invoke(null,
                    minecraft.getRenderViewEntity(), minecraft.playerController, reach,
                    false, partialTicks, true, shoulderCamera);
            if (result == null || result instanceof RayTraceResult) return (RayTraceResult) result;
            throw new IllegalStateException("Shoulder Surfing ray trace returned "
                    + result.getClass().getName() + " instead of RayTraceResult");
        } catch (ReflectiveOperationException | ClassCastException | LinkageError error) {
            throw compatibilityFailure("trace Shoulder camera ray", error);
        }
    }

    /** Returns the projected shoulder target in GUI space without changing the global GL matrix. */
    static float[] crosshairOffset() {
        if (!ShoulderSurfingMatrixFix.isTakingOver() || !isActive()) return null;
        if (ShoulderSurfingFixConfig.staticMode()) return null;
        if (ShoulderSurfingFixConfig.adaptive() && !usesPlayerAim()) return null;
        try {
            Object renderer = getRendererInstance.invoke(null);
            Object projected = rendererProjected.get(renderer);
            if (projected == null) return null;
            float projectedX = ((Number) translationX.invoke(projected)).floatValue();
            float projectedY = ((Number) translationY.invoke(projected)).floatValue();
            if (!Float.isFinite(projectedX) || !Float.isFinite(projectedY)) {
                throw new IllegalStateException("Shoulder Surfing produced a non-finite crosshair projection: "
                        + projectedX + ", " + projectedY);
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.displayWidth <= 0 || minecraft.displayHeight <= 0) return null;
            ScaledResolution resolution = new ScaledResolution(minecraft);
            float scaleX = resolution.getScaledWidth() / (float) minecraft.displayWidth;
            float scaleY = resolution.getScaledHeight() / (float) minecraft.displayHeight;
            float x = (projectedX - minecraft.displayWidth * 0.5F) * scaleX;
            float y = -(projectedY - minecraft.displayHeight * 0.5F) * scaleY;
            return new float[]{x, y};
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            throw compatibilityFailure("read projected crosshair", error);
        }
    }

    private static boolean isUsingBackportItem(EntityLivingBase entity) {
        return entity.isHandActive() && isAdaptiveItem(entity.getActiveItemStack());
    }

    private static boolean usesPlayerAim() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) return false;
        if (minecraft.player.isHandActive()
                && BackportCrosshairCompat.usesPlayerAim(minecraft.player.getActiveItemStack())) {
            return true;
        }
        return BackportCrosshairCompat.usesPlayerAim(minecraft.player.getHeldItemMainhand())
                || BackportCrosshairCompat.usesPlayerAim(minecraft.player.getHeldItemOffhand());
    }

    private static boolean usesCameraInteraction() {
        return ShoulderSurfingFixConfig.staticMode() || ShoulderSurfingFixConfig.dual()
                || ShoulderSurfingFixConfig.adaptive() && !usesPlayerAim();
    }

    private static boolean isAdaptiveItem(ItemStack stack) {
        return BackportCrosshairCompat.isCrossbow(stack)
                || BackportCrosshairCompat.isTrident(stack)
                || BackportCrosshairCompat.isSpyglass(stack);
    }

    private static synchronized void resolve() {
        if (resolved) return;
        try {
            Class<?> type = Class.forName("com.teamderpy.shouldersurfing.client.ShoulderInstance", false,
                    ShoulderSurfingCompat.class.getClassLoader());
            getInstance = type.getMethod("getInstance");
            isActive = type.getMethod("doShoulderSurfing");

            Class<?> renderer = Class.forName("com.teamderpy.shouldersurfing.client.ShoulderRenderer", false,
                    ShoulderSurfingCompat.class.getClassLoader());
            getRendererInstance = renderer.getMethod("getInstance");
            getPlayerReach = renderer.getMethod("getPlayerReach");
            rendererProjected = renderer.getDeclaredField("projected");
            rendererProjected.setAccessible(true);

            Class<?> vec2f = Class.forName("com.teamderpy.shouldersurfing.math.Vec2f", false,
                    ShoulderSurfingCompat.class.getClassLoader());
            translationX = vec2f.getMethod("getX");
            translationY = vec2f.getMethod("getY");

            Class<?> helper = Class.forName("com.teamderpy.shouldersurfing.client.ShoulderHelper", false,
                    ShoulderSurfingCompat.class.getClassLoader());
            traceBlocksAndEntities = helper.getMethod("traceBlocksAndEntities",
                    Entity.class, PlayerControllerMP.class, double.class, boolean.class,
                    float.class, boolean.class, boolean.class);
            resolved = true;
        } catch (ReflectiveOperationException | LinkageError error) {
            throw compatibilityFailure("resolve required Shoulder Surfing 1.12.2 API", error);
        }
    }

    private static RuntimeException compatibilityFailure(String operation, Throwable error) {
        Throwable cause = error instanceof InvocationTargetException
                && ((InvocationTargetException) error).getCause() != null
                ? ((InvocationTargetException) error).getCause() : error;
        LOGGER.error("Installed Shoulder Surfing compatibility failed while attempting to {}",
                operation, cause);
        if (cause instanceof RuntimeException) return (RuntimeException) cause;
        return new IllegalStateException("Installed Shoulder Surfing compatibility failed: "
                + operation, cause);
    }
}
