package neofontrender.addons.camera;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import neofontrender.addons.api.camera.CameraVector;

import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Linkage-free adapter for the Valkyrien Skies 1.12 ship transform and ray-tracer hooks. */
final class ValkyrienCameraCompat {
    private static final Logger LOGGER = LogManager.getLogger("UIE Valkyrien Camera Compat");
    private static boolean resolved;
    private static boolean available;
    private static RuntimeException runtimeResolutionFailure;
    private static LinkageError linkageResolutionFailure;
    private static Method mountedData;
    private static Object subspaceToGlobal;

    private ValkyrienCameraCompat() {}

    static CameraVector rotateMountedOffset(Entity entity, CameraVector offset) {
        resolve();
        if (mountedData == null || entity == null || offset == null) return offset;
        try {
            Object data = mountedData.invoke(null, entity);
            Object ship = invoke(data, "getMountedShip");
            Object mounted = invoke(data, "isMounted");
            if (ship == null || !Boolean.TRUE.equals(mounted)) return offset;
            Object manager = invoke(ship, "getShipTransformationManager");
            Object transform = invoke(manager, "getRenderTransform");
            Method rotate = find(transform, "rotate", 2);
            if (rotate == null || subspaceToGlobal == null)
                throw new NoSuchMethodException("Valkyrien render transform rotate hook");
            Object value = rotate.invoke(transform, new Vec3d(offset.x, offset.y, offset.z), subspaceToGlobal);
            if (!(value instanceof Vec3d)) return offset;
            Vec3d vector = (Vec3d) value;
            return new CameraVector(vector.x, vector.y, vector.z);
        } catch (ReflectiveOperationException error) {
            throw reflectionFailure("rotate mounted camera offset", error);
        } catch (RuntimeException error) {
            throw runtimeFailure("rotate mounted camera offset", error);
        } catch (LinkageError error) {
            throw linkageFailure("rotate mounted camera offset", error);
        }
    }

    static Object excludePilotedShip(World world, Entity player) {
        if (ShoulderCameraConfig.valkyrienShipCollision || world == null || player == null) return null;
        resolve();
        if (!available) return null;
        try {
            Object ship = invoke(player, "getPilotedShip");
            if (ship == null) return null;
            Method exclude = find(world, "excludeShipFromRayTracer", 1);
            if (exclude == null) throw new NoSuchMethodException("excludeShipFromRayTracer");
            exclude.invoke(world, ship);
            return ship;
        } catch (ReflectiveOperationException error) {
            throw reflectionFailure("exclude piloted ship from ray tracer", error);
        } catch (RuntimeException error) {
            throw runtimeFailure("exclude piloted ship from ray tracer", error);
        } catch (LinkageError error) {
            throw linkageFailure("exclude piloted ship from ray tracer", error);
        }
    }

    static void restorePilotedShip(World world, Object ship) {
        if (world == null || ship == null) return;
        try {
            Method restore = find(world, "unexcludeShipFromRayTracer", 1);
            if (restore == null) throw new NoSuchMethodException("unexcludeShipFromRayTracer");
            restore.invoke(world, ship);
        } catch (ReflectiveOperationException error) {
            throw reflectionFailure("restore piloted ship to ray tracer", error);
        } catch (RuntimeException error) {
            throw runtimeFailure("restore piloted ship to ray tracer", error);
        } catch (LinkageError error) {
            throw linkageFailure("restore piloted ship to ray tracer", error);
        }
    }

    private static synchronized void resolve() {
        if (resolved) {
            if (runtimeResolutionFailure != null) throw runtimeResolutionFailure;
            if (linkageResolutionFailure != null) throw linkageResolutionFailure;
            return;
        }
        resolved = true;
        ClassLoader loader = ValkyrienCameraCompat.class.getClassLoader();
        if (loader == null || loader.getResource(
                "org/valkyrienskies/mod/common/ValkyrienSkiesMod.class") == null) return;
        try {
            Class<?> utils = Class.forName("org.valkyrienskies.mod.common.util.ValkyrienUtils", false, loader);
            mountedData = utils.getMethod("getMountedShipAndPos", Entity.class);
            Class<?> transformType = Class.forName("valkyrienwarfare.api.TransformType", false, loader);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object value = Enum.valueOf((Class<? extends Enum>) transformType.asSubclass(Enum.class),
                    "SUBSPACE_TO_GLOBAL");
            subspaceToGlobal = value;
            available = true;
        } catch (ReflectiveOperationException error) {
            available = false;
            mountedData = null;
            subspaceToGlobal = null;
            runtimeResolutionFailure = reflectionFailure("resolve Valkyrien camera hooks", error);
            throw runtimeResolutionFailure;
        } catch (RuntimeException error) {
            available = false;
            mountedData = null;
            subspaceToGlobal = null;
            runtimeResolutionFailure = runtimeFailure("resolve Valkyrien camera hooks", error);
            throw runtimeResolutionFailure;
        } catch (LinkageError error) {
            available = false;
            mountedData = null;
            subspaceToGlobal = null;
            linkageResolutionFailure = linkageFailure("resolve Valkyrien camera hooks", error);
            throw linkageResolutionFailure;
        }
    }

    static boolean isAvailable() {
        resolve();
        return available;
    }

    private static Object invoke(Object target, String name) throws ReflectiveOperationException {
        if (target == null) return null;
        Method method = find(target, name, 0);
        if (method == null) throw new NoSuchMethodException(name);
        return method.invoke(target);
    }

    private static Method find(Object target, String name, int parameters) {
        if (target == null) return null;
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == parameters) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static IllegalStateException reflectionFailure(String operation,
                                                           ReflectiveOperationException error) {
        LOGGER.error("Failed to {}", operation, error);
        return new IllegalStateException("Failed to " + operation, error);
    }

    private static RuntimeException runtimeFailure(String operation, RuntimeException error) {
        LOGGER.error("Failed to {}", operation, error);
        return error;
    }

    private static LinkageError linkageFailure(String operation, LinkageError error) {
        LOGGER.error("Failed to {}", operation, error);
        return error;
    }
}
