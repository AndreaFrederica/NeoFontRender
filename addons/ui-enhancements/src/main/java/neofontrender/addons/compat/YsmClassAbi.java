package neofontrender.addons.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Verifies the exact YSM 1.1.9 renderer/head-data seam from class resources, without loading YSM. */
public final class YsmClassAbi {
    public static final String MOD_ID = "yes_steve_model";
    public static final String RENDERER =
            "com/elfmcys/yesstevemodel/geckolib3/geo/GeoReplacedEntityRenderer";
    public static final String ANIMATABLE =
            "com/elfmcys/yesstevemodel/geckolib3/core/IAnimatable";
    public static final String MODEL_DATA =
            "com/elfmcys/yesstevemodel/geckolib3/model/provider/data/EntityModelData";
    public static final String CUSTOM_PLAYER_MODEL =
            "com/elfmcys/yesstevemodel/client/model/CustomPlayerModel";

    public static final String RENDER_DESCRIPTOR =
            "(Lnet/minecraft/entity/EntityLivingBase;L" + ANIMATABLE + ";DDDFF)V";
    public static final String ROOT_METHOD = "func_77043_a";
    public static final String ROOT_DESCRIPTOR =
            "(Lnet/minecraft/entity/EntityLivingBase;FFF)V";
    public static final String CODE_ANIMATION_DESCRIPTOR =
            "(Lcom/elfmcys/yesstevemodel/geckolib3/core/event/predicate/AnimationEvent;L"
                    + MODEL_DATA + ";Lnet/minecraft/entity/player/EntityPlayer;)V";

    private YsmClassAbi() {}

    /**
     * Class-resource absence is distinct from a present but unsupported installation. Any read or
     * bytecode parse fault throws with its original cause, so it cannot silently look absent.
     */
    public static Result inspect(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        ClassNode renderer = read(loader, RENDERER);
        if (renderer == null) {
            if (loader.getResource(MODEL_DATA + ".class") != null
                    || loader.getResource(CUSTOM_PLAYER_MODEL + ".class") != null) {
                return Result.unsupported("YSM marker classes are present but "
                        + RENDERER + ".class is missing");
            }
            return Result.absent();
        }

        String mismatch = verifyRenderer(renderer);
        if (mismatch != null) return Result.unsupported(mismatch);

        ClassNode modelData = read(loader, MODEL_DATA);
        if (modelData == null) return Result.unsupported(MODEL_DATA + ".class is missing");
        mismatch = verifyModelData(modelData);
        if (mismatch != null) return Result.unsupported(mismatch);

        ClassNode customModel = read(loader, CUSTOM_PLAYER_MODEL);
        if (customModel == null) {
            return Result.unsupported(CUSTOM_PLAYER_MODEL + ".class is missing");
        }
        mismatch = verifyCustomPlayerModel(customModel);
        if (mismatch != null) return Result.unsupported(mismatch);
        return Result.supportedResult();
    }

    /**
     * Reads YSM's optional version metadata from the same JAR that supplied a YSM class resource.
     * This is safe before Forge's indexed mod list is ready and never loads a YSM class.
     */
    public static VersionMetadata versionMetadata(ClassLoader loader) {
        Objects.requireNonNull(loader, "loader");
        URL classResource = firstResource(loader, RENDERER, MODEL_DATA, CUSTOM_PLAYER_MODEL);
        if (classResource == null) {
            return VersionMetadata.unavailable("no YSM renderer/model class resource is available", null);
        }

        try {
            URLConnection classConnection = classResource.openConnection();
            classConnection.setUseCaches(false);
            if (!(classConnection instanceof JarURLConnection jarConnection)) {
                return VersionMetadata.unavailable("YSM class resource is not backed by a JAR", null);
            }

            URL metadataUrl = new URL("jar:" + jarConnection.getJarFileURL().toExternalForm()
                    + "!/mcmod.info");
            URLConnection metadataConnection = metadataUrl.openConnection();
            metadataConnection.setUseCaches(false);
            try (InputStream input = metadataConnection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonElement root = new JsonParser().parse(reader);
                if (!root.isJsonArray()) {
                    return VersionMetadata.unavailable("mcmod.info is not a JSON metadata array", null);
                }
                JsonArray mods = root.getAsJsonArray();
                for (JsonElement element : mods) {
                    if (!element.isJsonObject()) continue;
                    JsonObject mod = element.getAsJsonObject();
                    if (mod.has("modid") && MOD_ID.equals(mod.get("modid").getAsString())) {
                        if (mod.has("version") && mod.get("version").isJsonPrimitive()) {
                            String version = mod.get("version").getAsString();
                            if (!version.isBlank()) return VersionMetadata.available(version);
                        }
                        return VersionMetadata.unavailable(
                                "mcmod.info has no non-empty version for " + MOD_ID, null);
                    }
                }
                return VersionMetadata.unavailable("mcmod.info has no entry for " + MOD_ID, null);
            }
        } catch (IOException | RuntimeException error) {
            return VersionMetadata.unavailable("could not read or parse mcmod.info", error);
        }
    }

    private static URL firstResource(ClassLoader loader, String... internalNames) {
        for (String internalName : internalNames) {
            URL resource = loader.getResource(internalName + ".class");
            if (resource != null) return resource;
        }
        return null;
    }

    private static ClassNode read(ClassLoader loader, String internalName) {
        String resource = internalName + ".class";
        URL url = loader.getResource(resource);
        if (url == null) return null;
        try {
            URLConnection connection = url.openConnection();
            // Mod class resources often come from mod JARs. Avoid URL/JarFile caching so the
            // resource stream has a clear owner and can be closed when this gate finishes.
            connection.setUseCaches(false);
            try (InputStream input = connection.getInputStream()) {
                ClassNode node = new ClassNode(Opcodes.ASM9);
                new ClassReader(input).accept(node,
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return node;
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not read YSM class resource " + resource
                    + " from " + url, error);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Could not parse YSM class resource " + resource
                    + " from " + url, error);
        }
    }

    private static String verifyRenderer(ClassNode renderer) {
        if (!RENDERER.equals(renderer.name)) return "renderer resource declares " + renderer.name;
        MethodNode render = findMethod(renderer, "render", RENDER_DESCRIPTOR);
        if (render == null) return "missing " + RENDERER + ".render" + RENDER_DESCRIPTOR;
        if (countRootCalls(render) != 1) {
            return RENDERER + ".render must contain exactly one " + ROOT_METHOD + ROOT_DESCRIPTOR
                    + " call; found " + countRootCalls(render);
        }
        if (countFieldWrites(render, "headPitch") != 1) {
            return RENDERER + ".render must write EntityModelData.headPitch:F exactly once";
        }
        if (countFieldWrites(render, "netHeadYaw") != 1) {
            return RENDERER + ".render must write EntityModelData.netHeadYaw:F exactly once";
        }
        MethodNode root = findMethod(renderer, ROOT_METHOD, ROOT_DESCRIPTOR);
        if (root == null || (root.access & Opcodes.ACC_PROTECTED) == 0
                || (root.access & Opcodes.ACC_STATIC) != 0) {
            return "missing protected instance method " + RENDERER + "." + ROOT_METHOD
                    + ROOT_DESCRIPTOR;
        }
        return null;
    }

    private static int countRootCalls(MethodNode render) {
        int count = 0;
        for (var instruction = render.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && RENDERER.equals(call.owner)
                    && ROOT_METHOD.equals(call.name)
                    && ROOT_DESCRIPTOR.equals(call.desc)) count++;
        }
        return count;
    }

    private static int countFieldWrites(MethodNode render, String fieldName) {
        int count = 0;
        for (var instruction = render.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD
                    && MODEL_DATA.equals(field.owner)
                    && fieldName.equals(field.name)
                    && "F".equals(field.desc)) count++;
        }
        return count;
    }

    private static String verifyModelData(ClassNode modelData) {
        if (!MODEL_DATA.equals(modelData.name)) return "model-data resource declares " + modelData.name;
        if (!hasPublicFloatField(modelData, "headPitch")
                || !hasPublicFloatField(modelData, "netHeadYaw")) {
            return MODEL_DATA + " must expose public float headPitch/netHeadYaw fields";
        }
        return null;
    }

    private static boolean hasPublicFloatField(ClassNode owner, String name) {
        for (FieldNode field : owner.fields) {
            if (name.equals(field.name) && "F".equals(field.desc)
                    && (field.access & Opcodes.ACC_PUBLIC) != 0) return true;
        }
        return false;
    }

    private static String verifyCustomPlayerModel(ClassNode model) {
        if (!CUSTOM_PLAYER_MODEL.equals(model.name)) {
            return "custom-player-model resource declares " + model.name;
        }
        MethodNode codeAnimation = findMethod(model, "codeAnimation", CODE_ANIMATION_DESCRIPTOR);
        if (codeAnimation == null) {
            return "missing " + CUSTOM_PLAYER_MODEL + ".codeAnimation"
                    + CODE_ANIMATION_DESCRIPTOR;
        }
        boolean getsHead = false;
        boolean readsPitch = false;
        boolean readsYaw = false;
        boolean addsRotationX = false;
        boolean addsRotationY = false;
        for (var instruction = codeAnimation.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof LdcInsnNode constant && "Head".equals(constant.cst)) {
                getsHead = true;
            }
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.GETFIELD
                    && MODEL_DATA.equals(field.owner)) {
                readsPitch |= "headPitch".equals(field.name) && "F".equals(field.desc);
                readsYaw |= "netHeadYaw".equals(field.name) && "F".equals(field.desc);
            }
            if (instruction instanceof MethodInsnNode call) {
                addsRotationX |= "setRotationX".equals(call.name);
                addsRotationY |= "setRotationY".equals(call.name);
            }
        }
        if (!getsHead || !readsPitch || !readsYaw || !addsRotationX || !addsRotationY) {
            return CUSTOM_PLAYER_MODEL + ".codeAnimation must apply both EntityModelData angles to Head";
        }
        return null;
    }

    private static MethodNode findMethod(ClassNode owner, String name, String descriptor) {
        for (MethodNode method : owner.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }

    public static final class Result {
        public enum Status { ABSENT, SUPPORTED, UNSUPPORTED }

        private final Status status;
        private final String reason;

        private Result(Status status, String reason) {
            this.status = status;
            this.reason = reason;
        }

        private static Result absent() { return new Result(Status.ABSENT, "YSM classes absent"); }
        private static Result supportedResult() {
            return new Result(Status.SUPPORTED, "verified ABI");
        }
        private static Result unsupported(String reason) {
            return new Result(Status.UNSUPPORTED, reason);
        }

        public Status status() { return status; }
        public String reason() { return reason; }
        public boolean supported() { return status == Status.SUPPORTED; }
    }

    public static final class VersionMetadata {
        public static final String UNAVAILABLE = "unavailable";

        private final String version;
        private final String reason;
        private final Throwable cause;

        private VersionMetadata(String version, String reason, Throwable cause) {
            this.version = version;
            this.reason = reason;
            this.cause = cause;
        }

        private static VersionMetadata available(String version) {
            return new VersionMetadata(version, "mcmod.info", null);
        }

        private static VersionMetadata unavailable(String reason, Throwable cause) {
            return new VersionMetadata(UNAVAILABLE, reason, cause);
        }

        public String version() { return version; }
        public String reason() { return reason; }
        public Throwable cause() { return cause; }
    }
}
