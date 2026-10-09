package neofontrender.addons.mixin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.jar.JarFile;

import neofontrender.addons.compat.YsmClassAbi;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * One-shot test entry point for the actual Cleanroom 0.6.8 client libraries and remap pipeline.
 * It injects FMLTweaker but deliberately never asks Launch to execute a Minecraft launch target.
 */
public final class Cleanroom068MixinTransformProbe {
    private static final String RENDER_PLAYER =
            "net.minecraft.client.renderer.entity.RenderPlayer";
    private static final String YSM_RENDERER =
            "com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer";
    private static final String FML_TWEAKER =
            "net.minecraftforge.fml.common.launcher.FMLTweaker";
    private static final Map<String, byte[]> TRANSFORMED_BYTES = new ConcurrentHashMap<>();

    private Cleanroom068MixinTransformProbe() {}

    public static void main(String[] arguments) throws Throwable {
        if (arguments.length == 3 && "--verify-captured-hooks".equals(arguments[0])) {
            verifyCapturedHookBytecode(Path.of(arguments[1]), Path.of(arguments[2]));
            return;
        }
        if (arguments.length != 9) {
            throw new IllegalArgumentException("Expected clientJar, ysmJar-or-dash, gameDir, assetsDir, "
                    + "profile, ysmPresent, ysmEnabled, stagedReleaseJar, stagedCoreJar, "
                    + "or --verify-captured-hooks vanillaClass ysmClass");
        }
        Path clientJar = Path.of(arguments[0]).toAbsolutePath().normalize();
        boolean ysmPresent = parseBooleanArgument(arguments[5], "ysmPresent");
        boolean ysmEnabled = parseBooleanArgument(arguments[6], "ysmEnabled");
        Path ysmJar = ysmPresent ? Path.of(arguments[1]).toAbsolutePath().normalize() : null;
        if (!ysmPresent && !"-".equals(arguments[1])) {
            throw new IllegalArgumentException("Absent YSM variant must not stage a YSM JAR");
        }
        Path gameDir = Path.of(arguments[2]).toAbsolutePath().normalize();
        Path assetsDir = Path.of(arguments[3]).toAbsolutePath().normalize();
        Path stagedReleaseJar = Path.of(arguments[7]).toAbsolutePath().normalize();
        Path stagedCoreJar = Path.of(arguments[8]).toAbsolutePath().normalize();
        String profile = arguments[4];
        stage("input validation");
        requireRegularFile(clientJar, "Cleanroom client version JAR");
        requireRegularFile(stagedReleaseJar, "staged UIE release JAR");
        requireRegularFile(stagedCoreJar, "staged NFR core JAR");
        if (ysmPresent) requireRegularFile(ysmJar, "pinned YSM JAR");
        Files.createDirectories(gameDir);
        Files.createDirectories(assetsDir);
        System.out.println("PROBE_GAME_DIR=" + gameDir);
        System.out.println("PROBE_WORKING_DIR=" + Path.of(System.getProperty("user.dir")));
        System.out.println("PROBE_YSM_PRESENT=" + ysmPresent);
        System.out.println("PROBE_YSM_BODY_POSE_ENABLED=" + ysmEnabled);

        stage("construct actual LaunchClassLoader");
        Class<?> launchType = Class.forName("net.minecraft.launchwrapper.Launch");
        Class<?> launchLoaderType = Class.forName("net.minecraft.launchwrapper.LaunchClassLoader");
        URL[] runtimeInputs = Stream.of(System.getProperty("java.class.path")
                        .split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(Path::of).map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (java.net.MalformedURLException error) {
                        throw new IllegalStateException("Invalid runtime classpath entry " + path, error);
                    }
                }).toArray(URL[]::new);
        Object launchLoader = launchLoaderType.getConstructor(URL[].class)
                .newInstance((Object) runtimeInputs);
        launchType.getField("classLoader").set(null, launchLoader);
        if (ysmPresent) {
            launchLoaderType.getMethod("addURL", URL.class).invoke(launchLoader, ysmJar.toUri().toURL());
        }
        bootstrapLaunchState(launchType, launchLoaderType, launchLoader, gameDir, assetsDir);
        reportCodeSource("net.minecraft.launchwrapper.Launch", launchType);
        reportCodeSource("net.minecraft.launchwrapper.LaunchClassLoader", launchLoaderType);
        reportCodeSource("org.spongepowered.asm.launch.MixinBootstrap",
                Class.forName("org.spongepowered.asm.launch.MixinBootstrap", false,
                        (ClassLoader) launchLoader));
        reportCodeSource("com.cleanroommc.cleanmix.service.CleanMixService",
                Class.forName("com.cleanroommc.cleanmix.service.CleanMixService", false,
                        (ClassLoader) launchLoader));
        reportCodeSource("com.llamalad7.mixinextras.MixinExtrasBootstrap",
                Class.forName("com.llamalad7.mixinextras.MixinExtrasBootstrap", false,
                        (ClassLoader) launchLoader));
        stage("process actual Foundation tweaker queue through pre-target setup");
        List<String> processedTweaks = processActualTweakerQueue(
                launchType, launchLoaderType, launchLoader, gameDir, assetsDir, profile);
        System.out.println("PROCESSED_TWEAKS=" + processedTweaks);
        if (!processedTweaks.contains(FML_TWEAKER)
                || !processedTweaks.contains("net.minecraftforge.fml.common.launcher.FMLInjectionAndSortingTweaker")
                || !processedTweaks.contains("net.minecraftforge.fml.common.launcher.FMLDeobfTweaker")) {
            throw new IllegalStateException("Actual FML tweak queue omitted a required bootstrap stage: "
                    + processedTweaks);
        }
        verifyCoremodLoaderState((ClassLoader) launchLoader, gameDir.resolve("mods"),
                stagedReleaseJar, stagedCoreJar, ysmJar);
        reportCodeSource("neofontrender.internal.nightconfig.core.file.CommentedFileConfig",
                Class.forName("neofontrender.internal.nightconfig.core.file.CommentedFileConfig",
                        false, (ClassLoader) launchLoader));
        printAndVerifyDeobfuscationTransformer(launchLoader);
        verifyMixinDefaultPhase((ClassLoader) launchLoader);

        stage("verify pre-mod FML config state without manual Loader initialization");
        Path actualConfigPath = gameDir.resolve("config").toAbsolutePath().normalize();
        Class<?> loaderType = Class.forName(
                "net.minecraftforge.fml.common.Loader", true, (ClassLoader) launchLoader);
        Object forgeLoader = invoke(loaderType.getMethod("instance"), null);
        File naturalConfigDir = (File) invoke(loaderType.getMethod("getConfigDir"), forgeLoader);
        if (naturalConfigDir != null
                && !naturalConfigDir.toPath().toAbsolutePath().normalize().equals(actualConfigPath)) {
            throw new IllegalStateException("Normal pre-mod Loader config path escaped probe: "
                    + naturalConfigDir + " (expected " + actualConfigPath + ")");
        }
        System.out.println("FML_CONFIG_DIR_BEFORE_LOAD_MODS=" + naturalConfigDir);
        System.out.println("EXPECTED_EARLY_CONFIG_DIR=" + actualConfigPath);

        installTransformedBytecodeObserver(launchLoader);

        stage("transform vanilla RenderPlayer");
        Class<?> renderPlayer = Class.forName(RENDER_PLAYER, false, (ClassLoader) launchLoader);
        requireDefinedBy(renderPlayer, (ClassLoader) launchLoader);
        ClassNode playerClass = capturedClass(RENDER_PLAYER);
        persistCapturedClass(gameDir.resolve("captured-classes"), RENDER_PLAYER);
        verifyVanillaPlayerHooks(playerClass);
        verifyFlightApi10((ClassLoader) launchLoader, gameDir);
        System.out.println("TRANSFORMED=" + RENDER_PLAYER);
        System.out.println("TRANSFORMED_HOOKS=" + methodNames(playerClass));
        System.out.println("TARGET_CODESOURCE=" + codeSource(renderPlayer));

        if (!ysmPresent) {
            ClassLoader actualLoader = (ClassLoader) launchLoader;
            if (actualLoader.getResource(YsmClassAbi.RENDERER + ".class") != null) {
                throw new IllegalStateException("YSM renderer resource unexpectedly exists in absent variant");
            }
            Class<?> compatType = Class.forName("neofontrender.addons.compat.YsmCompat", true, actualLoader);
            requireDefinedBy(compatType, actualLoader);
            Object compat = instantiate(compatType.getConstructor());
            boolean ysmMixinAllowed = (boolean) invoke(compatType.getMethod("shouldApplyMixin", String.class),
                    compat, "neofontrender.addons.mixin.MixinYsmPlayerBodyPose");
            if (ysmMixinAllowed) {
                throw new IllegalStateException("Actual YSM mixin gate allowed an absent YSM installation");
            }
            try {
                Class.forName(YSM_RENDERER, false, actualLoader);
                throw new IllegalStateException("YSM renderer class loaded although its JAR was not staged");
            } catch (ClassNotFoundException expectedAbsent) {
                System.out.println("YSM_CLASS_ABSENT=true");
            }
            System.out.println("YSM_GATE_RESULT=absent-vetoed");
            System.out.println("MIXIN_TRANSFORM=PASS");
            return;
        }

        stage("transform YSM renderer");
        Class<?> ysmRenderer = Class.forName(YSM_RENDERER, false, (ClassLoader) launchLoader);
        requireDefinedBy(ysmRenderer, (ClassLoader) launchLoader);
        ClassNode ysmClass = capturedClass(YSM_RENDERER);
        persistCapturedClass(gameDir.resolve("captured-classes"), YSM_RENDERER);
        Path ysmConfig = actualConfigPath.resolve("neofontrender-ui-enhancements.toml");
        boolean persistedBodyPoseEnabled = readYsmPoseConfigEnabled((ClassLoader) launchLoader);
        if (!ysmEnabled) {
            verifyNoYsmBodyPoseHooks(ysmClass);
            verifyNativeYsmRenderPath(ysmClass);
            if (!Files.isRegularFile(ysmConfig) || persistedBodyPoseEnabled) {
                throw new IllegalStateException("Installed YSM opt-out did not remain false in " + ysmConfig);
            }
            System.out.println("YSM_GATE_RESULT=installed-but-disabled");
        } else {
            verifyYsmPlayerHooks(ysmClass);
            if (!Files.isRegularFile(ysmConfig) || !persistedBodyPoseEnabled) {
                throw new IllegalStateException("YSM mixin gate did not persist default-enabled config: "
                        + ysmConfig);
            }
        }
        String config = Files.readString(ysmConfig);
        String compactConfig = config.replaceAll("\\s+", "");
        String expected = "enabled=" + ysmEnabled;
        if (!compactConfig.contains("ysmBodyPose") || !compactConfig.contains(expected)) {
            throw new IllegalStateException("YSM config was not persisted as " + expected + ": " + ysmConfig);
        }
        System.out.println("TRANSFORMED=" + YSM_RENDERER);
        System.out.println("TRANSFORMED_HOOKS=" + methodNames(ysmClass));
        System.out.println("TARGET_CODESOURCE=" + codeSource(ysmRenderer));
        System.out.println("YSM_CONFIG=" + ysmConfig);
        System.out.println("MIXIN_TRANSFORM=PASS");
    }

    private static boolean parseBooleanArgument(String value, String label) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IllegalArgumentException("Expected literal true/false for " + label + ", found: " + value);
    }

    private static void verifyNoYsmBodyPoseHooks(ClassNode ysm) {
        List<String> applied = ysm.methods.stream().map(method -> method.name + method.desc)
                .filter(name -> name.endsWith("$nfrUi$renderWithSampledBodyPose")
                        || name.endsWith("$nfrUi$replaceRootRotation")
                        || name.endsWith("$nfrUi$replaceHeadPitch")
                        || name.endsWith("$nfrUi$replaceHeadYaw"))
                .toList();
        if (!applied.isEmpty()) {
            throw new IllegalStateException("Disabled YSM integration still injected UIE hooks: " + applied);
        }
        System.out.println("YSM_UIE_HOOKS_ABSENT=true");
    }

    private static void verifyFlightApi10(ClassLoader launchLoader, Path gameDir) throws Throwable {
        Class<?> flightApi = Class.forName(
                "neofontrender.addons.api.flight.FlightApi", true, launchLoader);
        requireDefinedBy(flightApi, launchLoader);
        Path expectedReleaseJar = gameDir.resolve("mods/neofontrender-ui-enhancements.jar")
                .toAbsolutePath().normalize();
        requireCodeSource(flightApi, expectedReleaseJar);
        Object version = invoke(flightApi.getMethod("getApiVersion"), null);
        if (!Integer.valueOf(10).equals(version)) {
            throw new IllegalStateException("Loaded UI Enhancements flight API version is "
                    + version + ", expected 10; CodeSource=" + codeSource(flightApi));
        }

        Class<?> attitude = Class.forName(
                "neofontrender.addons.api.flight.FlightAttitude", false, launchLoader);
        Class<?> bodyPose = Class.forName(
                "neofontrender.addons.api.flight.FlightBodyPose", false, launchLoader);
        requireDefinedBy(bodyPose, launchLoader);
        bodyPose.getConstructor(attitude);
        bodyPose.getMethod("modelUpY", attitude);
        bodyPose.getMethod("modelUpY", attitude, attitude);

        Class<?> presentationPolicy = Class.forName(
                "neofontrender.addons.flight.FlightBodyPresentationPolicy", false, launchLoader);
        requireDefinedBy(presentationPolicy, launchLoader);
        Class<?> decision = Class.forName(
                "neofontrender.addons.flight.FlightBodyPresentationPolicy$Decision",
                false, launchLoader);
        requireDefinedBy(decision, launchLoader);
        presentationPolicy.getMethod("resolve", bodyPose, boolean.class);
        decision.getMethod("ownsRootQuaternion");
        decision.getMethod("rootAttitude");
        decision.getMethod("neutralizesRelativeHead");
        decision.getMethod("relativeHeadYawDegrees");
        decision.getMethod("relativeHeadPitchDegrees");
        decision.getMethod("isElytraFlying");

        System.out.println("FLIGHT_API_VERSION=" + version);
        System.out.println("FLIGHT_API_CODESOURCE=" + codeSource(flightApi));
        System.out.println("FLIGHT_BODY_POSE_API10_ABI=PASS");
        System.out.println("FLIGHT_BODY_PRESENTATION_POLICY_ABI=PASS");
    }

    private static void verifyNativeYsmRenderPath(ClassNode ysm) {
        MethodNode render = ysm.methods.stream().filter(method -> method.name.equals("render")
                && method.desc.equals(YsmClassAbi.RENDER_DESCRIPTOR)).findFirst().orElseThrow(() ->
                new IllegalStateException("Disabled YSM target lacks the actual render descriptor"));
        int rootCalls = 0;
        int headPitchWrites = 0;
        int headYawWrites = 0;
        for (var instruction = render.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && YsmClassAbi.RENDERER.equals(call.owner)
                    && YsmClassAbi.ROOT_METHOD.equals(call.name)
                    && YsmClassAbi.ROOT_DESCRIPTOR.equals(call.desc)) rootCalls++;
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD
                    && YsmClassAbi.MODEL_DATA.equals(field.owner)
                    && "F".equals(field.desc)) {
                if ("headPitch".equals(field.name)) headPitchWrites++;
                if ("netHeadYaw".equals(field.name)) headYawWrites++;
            }
        }
        if (rootCalls != 1 || headPitchWrites != 1 || headYawWrites != 1) {
            throw new IllegalStateException("Disabled YSM renderer did not retain its native root/head path: "
                    + "rootCalls=" + rootCalls + ", headPitchWrites=" + headPitchWrites
                    + ", headYawWrites=" + headYawWrites);
        }
        System.out.println("YSM_NATIVE_ROOT_HEAD_PATH=PASS");
    }

    /**
     * Replays Foundation 0.19.11 LaunchHandler's real queue-drain prefix and stops before
     * getLaunchTarget()/main. Every instantiated tweak and coremod wrapper is the host's object.
     */
    private static List<String> processActualTweakerQueue(Class<?> launchType,
                                                          Class<?> launchLoaderType,
                                                          Object launchLoader,
                                                          Path gameDir,
                                                          Path assetsDir,
                                                          String profile) throws Throwable {
        Object value = launchType.getField("blackboard").get(null);
        if (!(value instanceof Map<?, ?> blackboard)
                || !(blackboard.get("TweakClasses") instanceof List<?> rawRequestedTweaks)
                || !(blackboard.get("Tweaks") instanceof List<?> rawPendingTweaks)
                || !(blackboard.get("ArgumentList") instanceof List<?>)) {
            throw new IllegalStateException("Foundation tweak queue state is not initialized");
        }
        @SuppressWarnings("unchecked")
        List<String> requestedTweaks = (List<String>) rawRequestedTweaks;
        @SuppressWarnings("unchecked")
        List<Object> pendingTweaks = (List<Object>) rawPendingTweaks;
        if (!requestedTweaks.isEmpty() || !pendingTweaks.isEmpty()) {
            throw new IllegalStateException("Foundation queue must be fresh before its first tweak: "
                    + "requested=" + requestedTweaks + ", pending=" + pendingTweaks);
        }
        requestedTweaks.add(FML_TWEAKER);
        List<String> initiallyRequested = new ArrayList<>(requestedTweaks);
        Set<String> visitedTweaks = new HashSet<>();
        List<String> processedTweaks = new ArrayList<>();
        List<Object> processedInstances = new ArrayList<>();
        Object primaryTweak = null;
        Class<?> tweakerInterface = Class.forName("net.minecraft.launchwrapper.ITweaker",
                true, (ClassLoader) launchLoader);
        Method acceptOptions = tweakerInterface.getMethod("acceptOptions", List.class,
                File.class, File.class, String.class);
        Method injectIntoClassLoader = tweakerInterface.getMethod(
                "injectIntoClassLoader", launchLoaderType);
        Method addTransformerExclusion = launchLoaderType.getMethod(
                "addTransformerExclusion", String.class);

        while (true) {
            while (!requestedTweaks.isEmpty()) {
                String tweakName = requestedTweaks.getFirst();
                if (!visitedTweaks.add(tweakName)) {
                    requestedTweaks.removeFirst();
                    continue;
                }
                int packageEnd = tweakName.lastIndexOf('.');
                invoke(addTransformerExclusion, launchLoader,
                        tweakName.substring(0, packageEnd));
                Class<?> tweakType = Class.forName(tweakName, true, (ClassLoader) launchLoader);
                requireDefinedBy(tweakType, (ClassLoader) launchLoader);
                reportCodeSource(tweakName, tweakType);
                Object tweak = tweakType.getConstructor().newInstance();
                pendingTweaks.add(tweak);
                requestedTweaks.removeFirst();
                if (primaryTweak == null) primaryTweak = tweak;
            }

            while (!pendingTweaks.isEmpty()) {
                Object tweak = pendingTweaks.getFirst();
                invoke(acceptOptions, tweak, List.of(), gameDir.toFile(), assetsDir.toFile(), profile);
                invoke(injectIntoClassLoader, tweak, launchLoader);
                processedTweaks.add(tweak.getClass().getName());
                processedInstances.add(tweak);
                pendingTweaks.remove(tweak);
            }
            if (requestedTweaks.isEmpty()) break;
        }
        if (primaryTweak == null || !primaryTweak.getClass().getName().equals(FML_TWEAKER)) {
            throw new IllegalStateException("Foundation primary tweak is not the requested FMLTweaker: "
                    + (primaryTweak == null ? "null" : primaryTweak.getClass().getName()));
        }
        if (!pendingTweaks.isEmpty()) {
            throw new IllegalStateException("Foundation left unprocessed queued tweaks: " + pendingTweaks);
        }
        if (!requestedTweaks.isEmpty()) {
            throw new IllegalStateException("Foundation left unprocessed TweakClasses: " + requestedTweaks);
        }
        System.out.println("FML_INITIAL_TWEAK_QUEUE=" + initiallyRequested);
        System.out.println("FML_TWEAK_QUEUE_EMPTY=true");
        collectRealTweakArguments(launchType, (ClassLoader) launchLoader, processedInstances);
        return List.copyOf(processedTweaks);
    }

    /** Collects each actual tweaker's arguments, the last safe phase before Foundation loads main. */
    private static void collectRealTweakArguments(Class<?> launchType, ClassLoader launchLoader,
                                                  List<Object> processedTweaks) throws Throwable {
        Object value = launchType.getField("blackboard").get(null);
        if (!(value instanceof Map<?, ?> blackboard)
                || !(blackboard.get("ArgumentList") instanceof List<?> rawArguments)) {
            throw new IllegalStateException("Foundation ArgumentList is not initialized");
        }
        @SuppressWarnings("unchecked")
        List<Object> arguments = (List<Object>) rawArguments;
        Class<?> tweakerInterface = Class.forName("net.minecraft.launchwrapper.ITweaker",
                true, launchLoader);
        Method getLaunchArguments = tweakerInterface.getMethod("getLaunchArguments");
        List<String> argumentSources = new ArrayList<>();
        for (Object tweak : processedTweaks) {
            Object returned = invoke(getLaunchArguments, tweak);
            if (!(returned instanceof String[] tweakArguments)) {
                throw new IllegalStateException(tweak.getClass().getName()
                        + ".getLaunchArguments did not return String[]");
            }
            arguments.addAll(Arrays.asList(tweakArguments));
            argumentSources.add(tweak.getClass().getName());
        }
        System.out.println("FOUNDATION_ARGUMENT_SOURCES=" + argumentSources);
        System.out.println("FOUNDATION_ARGUMENT_COUNT=" + arguments.size());
    }

    private static void verifyCoremodLoaderState(ClassLoader launchLoader, Path modsDirectory,
                                                 Path stagedReleaseJar, Path stagedCoreJar,
                                                 Path stagedYsmJar) throws Throwable {
        stage("verify Cleanroom coremod registration and Mixin config ownership");
        Path actualModsDirectory = modsDirectory.toRealPath();
        List<Path> expectedCoremodJars = new ArrayList<>(List.of(stagedReleaseJar, stagedCoreJar));
        if (stagedYsmJar != null) expectedCoremodJars.add(stagedYsmJar);

        Class<?> coreModManager = Class.forName(
                "net.minecraftforge.fml.relauncher.CoreModManager", true, launchLoader);
        requireDefinedBy(coreModManager, launchLoader);
        Method isCoreModLoaded = coreModManager.getMethod("isCoreModLoaded", String.class);
        Class<?> loadingPlugin = Class.forName(
                "net.minecraftforge.fml.relauncher.IFMLLoadingPlugin", false, launchLoader);
        requireDefinedBy(loadingPlugin, launchLoader);

        for (Path expectedJar : expectedCoremodJars) {
            Path stagedJar = expectedJar.toRealPath();
            if (!stagedJar.startsWith(actualModsDirectory)) {
                throw new IllegalStateException("Expected coremod JAR is outside the isolated game/mods directory: "
                        + stagedJar + " (mods=" + actualModsDirectory + ")");
            }
            String pluginName = fmlCorePluginName(stagedJar);
            Object loaded = invoke(isCoreModLoaded, null, pluginName);
            if (!Boolean.TRUE.equals(loaded)) {
                throw new IllegalStateException("Cleanroom did not register the coremod from its staged JAR: "
                        + pluginName + " | " + stagedJar);
            }

            Class<?> pluginType = Class.forName(pluginName, false, launchLoader);
            requireDefinedBy(pluginType, launchLoader);
            if (!loadingPlugin.isAssignableFrom(pluginType)) {
                throw new IllegalStateException("Coremod plugin is incompatible with the actual host "
                        + "IFMLLoadingPlugin definition: " + pluginName + " | pluginLoader="
                        + pluginType.getClassLoader() + " | interfaceLoader=" + loadingPlugin.getClassLoader());
            }
            requireCodeSource(pluginType, stagedJar);
            System.out.println("COREMOD_REGISTERED=" + pluginName + "|loader=LaunchClassLoader|codesource="
                    + codeSource(pluginType));
        }

        // Mixins.getConfigs() is a pending queue that MixinProcessor consumes; use the
        // persistent config map and environment names to prove registration after that phase.
        Class<?> configType = Class.forName(
                "org.spongepowered.asm.mixin.transformer.Config", true, launchLoader);
        requireDefinedBy(configType, launchLoader);
        Object configValue = invoke(configType.getMethod("getAllConfigs"), null);
        if (!(configValue instanceof Map<?, ?> configs)) {
            throw new IllegalStateException("Actual persistent Mixin config registry returned an unexpected value: "
                    + (configValue == null ? "null" : configValue.getClass().getName()));
        }
        String requiredConfig = "mixins.neofontrender_ui_enhancements.json";
        Object config = configs.get(requiredConfig);
        if (config == null) {
            throw new IllegalStateException("Cleanroom did not retain the UIE Mixin config in the actual "
                    + "persistent config registry: " + requiredConfig + " | registered=" + configs.keySet());
        }
        requireDefinedBy(config.getClass(), launchLoader);
        Object configName = invoke(configType.getMethod("getName"), config);
        if (!requiredConfig.equals(configName)) {
            throw new IllegalStateException("Persistent Mixin config registry returned an unexpected config: "
                    + configName + " (requested " + requiredConfig + ")");
        }

        Class<?> environmentType = Class.forName(
                "org.spongepowered.asm.mixin.MixinEnvironment", true, launchLoader);
        requireDefinedBy(environmentType, launchLoader);
        Object environment = invoke(environmentType.getMethod("getCurrentEnvironment"), null);
        Object environmentConfigValue = invoke(environmentType.getMethod("getMixinConfigs"), environment);
        if (!(environmentConfigValue instanceof List<?> environmentConfigs)) {
            throw new IllegalStateException("Actual Mixin environment returned an unexpected config list: "
                    + (environmentConfigValue == null ? "null" : environmentConfigValue.getClass().getName()));
        }
        if (!environmentConfigs.contains(requiredConfig)) {
            throw new IllegalStateException("UIE Mixin config is absent from the actual environment's persistent "
                    + "registered config names: " + requiredConfig + " | registered=" + environmentConfigs);
        }
        System.out.println("UIE_MIXIN_CONFIG_REGISTERED=" + requiredConfig);
        System.out.println("ACTUAL_MIXIN_ENVIRONMENT_CONFIGS=" + environmentConfigs);
    }

    private static String fmlCorePluginName(Path stagedJar) throws java.io.IOException {
        try (JarFile jarFile = new JarFile(stagedJar.toFile())) {
            java.util.jar.Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                throw new IllegalStateException("Staged coremod JAR has no manifest: " + stagedJar);
            }
            String pluginName = manifest.getMainAttributes().getValue("FMLCorePlugin");
            if (pluginName == null || pluginName.isBlank()) {
                throw new IllegalStateException("Staged coremod JAR has no FMLCorePlugin manifest attribute: "
                        + stagedJar);
            }
            return pluginName.trim();
        }
    }

    private static void verifyMixinDefaultPhase(ClassLoader launchLoader) throws Throwable {
        Class<?> environmentType = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment",
                true, launchLoader);
        Object environment = invoke(environmentType.getMethod("getCurrentEnvironment"), null);
        Object phase = invoke(environmentType.getMethod("getPhase"), environment);
        if (!"DEFAULT".equals(String.valueOf(phase))) {
            throw new IllegalStateException("Foundation tweaker argument collection did not advance "
                    + "MixinEnvironment to DEFAULT; observed " + phase);
        }
        System.out.println("MIXIN_ENVIRONMENT_PHASE=" + phase);
    }

    private static void printAndVerifyDeobfuscationTransformer(Object launchLoader) throws Throwable {
        Method getTransformers = launchLoader.getClass().getMethod("getTransformers");
        Object installed = invoke(getTransformers, launchLoader);
        if (!(installed instanceof Iterable<?> transformers)) {
            throw new IllegalStateException("Foundation transformer list has unexpected type: "
                    + (installed == null ? "null" : installed.getClass().getName()));
        }
        boolean hasDeobfuscationTransformer = false;
        List<String> installedNames = new ArrayList<>();
        for (Object transformer : transformers) {
            installedNames.add(transformer.getClass().getName());
            if (transformer.getClass().getName().equals(
                    "net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer")) {
                hasDeobfuscationTransformer = true;
            }
        }
        if (!hasDeobfuscationTransformer) {
            throw new IllegalStateException("Foundation tweak queue completed without installing "
                    + "the real DeobfuscationTransformer; installed=" + installedNames);
        }
        System.out.println("FML_DEOBF_TRANSFORMER=INSTALLED");
        System.out.println("FOUNDATION_CLASS_TRANSFORMERS=" + installedNames);
    }

    /** Adds a no-op observer after the host's real transformers to retain their final class bytes. */
    private static void installTransformedBytecodeObserver(Object launchLoader) throws Throwable {
        ClassLoader loader = (ClassLoader) launchLoader;
        Class<?> transformerInterface = Class.forName(
                "net.minecraft.launchwrapper.IClassTransformer", true, loader);
        Object observer = Proxy.newProxyInstance(loader, new Class<?>[]{transformerInterface},
                (proxy, method, arguments) -> {
                    if ("transform".equals(method.getName()) && arguments != null
                            && arguments.length == 3) {
                        String originalName = (String) arguments[0];
                        String transformedName = (String) arguments[1];
                        byte[] bytes = (byte[]) arguments[2];
                        String capturedName = isCaptureTarget(originalName) ? originalName
                                : isCaptureTarget(transformedName) ? transformedName : null;
                        if (capturedName != null && bytes != null) {
                            TRANSFORMED_BYTES.put(capturedName, bytes.clone());
                        }
                        return bytes;
                    }
                    return switch (method.getName()) {
                        case "toString" -> "UIE transformed-bytecode observer";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == arguments[0];
                        default -> throw new UnsupportedOperationException(
                                "Unexpected transformer proxy method " + method);
                    };
                });
        Class<?> delegateType = Class.forName("top.outlands.foundation.TransformerDelegate",
                true, loader);
        Object value = invoke(delegateType.getMethod("getTransformers"), null);
        if (!(value instanceof List<?> rawTransformers)) {
            throw new IllegalStateException("Foundation transformer list has unexpected type: "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        @SuppressWarnings("unchecked")
        List<Object> transformers = (List<Object>) rawTransformers;
        transformers.add(observer);
        System.out.println("TRANSFORM_BYTECODE_OBSERVER_INDEX=" + (transformers.size() - 1));
    }

    private static boolean isCaptureTarget(String name) {
        if (name == null) return false;
        String binaryName = name.replace('/', '.');
        return RENDER_PLAYER.equals(binaryName) || YSM_RENDERER.equals(binaryName);
    }

    private static ClassNode capturedClass(String binaryName) {
        String internalName = binaryName.replace('.', '/');
        byte[] bytes = TRANSFORMED_BYTES.get(internalName);
        if (bytes == null) bytes = TRANSFORMED_BYTES.get(binaryName);
        if (bytes == null) {
            throw new IllegalStateException("The actual transformer chain did not expose final bytes for "
                    + binaryName);
        }
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (!internalName.equals(node.name)) {
            throw new IllegalStateException("Captured " + binaryName + " bytes declare " + node.name);
        }
        System.out.println("CAPTURED_TRANSFORMED_CLASS=" + binaryName + "|bytes=" + bytes.length);
        return node;
    }

    private static void persistCapturedClass(Path captureDirectory, String binaryName)
            throws java.io.IOException {
        byte[] bytes = TRANSFORMED_BYTES.get(binaryName.replace('.', '/'));
        if (bytes == null) bytes = TRANSFORMED_BYTES.get(binaryName);
        if (bytes == null) {
            throw new IllegalStateException("Cannot persist uncaptured transformed class " + binaryName);
        }
        Path classFile = captureDirectory.resolve(binaryName.replace('.', '/') + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, bytes);
        System.out.println("CAPTURED_CLASS_FILE=" + classFile.toAbsolutePath().normalize()
                + "|sha256=" + sha256(bytes) + "|bytes=" + bytes.length);
    }

    private static void verifyCapturedHookBytecode(Path vanillaClassFile, Path ysmClassFile)
            throws java.io.IOException {
        ClassNode vanilla = readCapturedClass(vanillaClassFile, RENDER_PLAYER);
        verifyVanillaPlayerHooks(vanilla);
        ClassNode ysm = readCapturedClass(ysmClassFile, YSM_RENDERER);
        verifyYsmPlayerHooks(ysm);
        System.out.println("OFFLINE_CAPTURED_HOOK_VALIDATION=PASS");
    }

    private static ClassNode readCapturedClass(Path classFile, String binaryName)
            throws java.io.IOException {
        byte[] bytes = Files.readAllBytes(classFile);
        ClassNode node = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        String expected = binaryName.replace('.', '/');
        if (!expected.equals(node.name)) {
            throw new IllegalStateException("Captured class file " + classFile + " declares "
                    + node.name + ", expected " + expected);
        }
        System.out.println("OFFLINE_CAPTURED_CLASS=" + binaryName + "|file="
                + classFile.toAbsolutePath().normalize() + "|sha256=" + sha256(bytes)
                + "|bytes=" + bytes.length);
        return node;
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().withUpperCase().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new AssertionError("SHA-256 is required by the Java platform", impossible);
        }
    }

    private static void verifyVanillaPlayerHooks(ClassNode player) {
        printMethods(player, Set.of("func_76986_a", "func_77043_a", "doRender", "applyRotations"));
        requireExactlyOne(player, "body render wrapper",
                method -> method.name.startsWith("wrapMethod$")
                        && method.name.endsWith("$nfrUi$renderWithSampledBodyPose"));
        requireExactlyOne(player, "elytra/root head injection",
                method -> method.name.startsWith("handler$")
                        && method.name.endsWith("$nfrUi$applyLongitudinalRoll"));
        requireExactlyOne(player, "non-elytra roll injection",
                method -> method.name.startsWith("handler$")
                        && method.name.endsWith("$nfrUi$applyNonElytraLongitudinalRoll"));
        requireExactMethod(player, "nfrUi$mayOwnRoot");
        requireExactMethod(player, "nfrUi$rotate");
        requireExactMethod(player, "nfrUi$restore");
        requireExactMethod(player, "nfrUi$snapshot");
        requireExactMethod(player, "nfrUi$applySampledHead");
        requireRenderCallsite(player, "func_76986_a",
                "(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V",
                "sampled body-pose WrapMethod",
                call -> call.name.startsWith("wrapMethod$")
                        && call.name.endsWith("$nfrUi$renderWithSampledBodyPose"));
        requireRenderCallsite(player, "func_77043_a",
                "(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
                "body root head injection",
                call -> call.name.startsWith("handler$")
                        && call.name.endsWith("$nfrUi$applyLongitudinalRoll"));
        printCallGraph(player, Set.of("func_76986_a", "func_77043_a"));
    }

    private static void verifyYsmPlayerHooks(ClassNode ysm) {
        String operationDescriptor = "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;";
        String rootOperationDescriptor = "(Ljava/lang/Object;Lnet/minecraft/entity/EntityLivingBase;"
                + "FFF" + operationDescriptor + ")V";
        String headOperationDescriptor = "(Ljava/lang/Object;F" + operationDescriptor + ")V";
        String renderDescriptor = "(Lnet/minecraft/entity/EntityLivingBase;"
                + "Lcom/elfmcys/yesstevemodel/geckolib3/core/IAnimatable;DDDFF)V";
        String renderScopeDescriptor = "(Lnet/minecraft/entity/EntityLivingBase;Ljava/lang/Object;"
                + "DDDFF" + operationDescriptor + ")V";
        Predicate<MethodNode> renderScope = method -> method.name.startsWith("wrapMethod$")
                && method.name.endsWith("$nfrUi$renderWithSampledBodyPose")
                && method.desc.equals(renderScopeDescriptor);
        Predicate<MethodNode> rootOperation = method -> method.name.startsWith("wrapOperation$")
                && method.name.endsWith("$nfrUi$replaceRootRotation")
                && method.desc.equals(rootOperationDescriptor);
        Predicate<MethodNode> headPitchOperation = method -> method.name.startsWith("wrapOperation$")
                && method.name.endsWith("$nfrUi$replaceHeadPitch")
                && method.desc.equals(headOperationDescriptor);
        Predicate<MethodNode> headYawOperation = method -> method.name.startsWith("wrapOperation$")
                && method.name.endsWith("$nfrUi$replaceHeadYaw")
                && method.desc.equals(headOperationDescriptor);
        requireExactlyOne(ysm, "YSM render scope wrapper", renderScope);
        requireExactlyOne(ysm, "YSM root rotation operation", rootOperation);
        requireExactlyOne(ysm, "YSM head pitch operation", headPitchOperation);
        requireExactlyOne(ysm, "YSM head yaw operation", headYawOperation);
        requireExactMethod(ysm, "nfrUi$mayOwnRoot");
        requireExactMethod(ysm, "nfrUi$rotate");
        requireRenderCallsite(ysm, "render", renderDescriptor, "sampled YSM render scope",
                call -> call.name.startsWith("wrapMethod$")
                        && call.name.endsWith("$nfrUi$renderWithSampledBodyPose")
                        && call.desc.equals(renderScopeDescriptor));
        requireReachableCallsite(ysm, "render", renderDescriptor, "YSM root rotation operation",
                call -> call.name.startsWith("wrapOperation$")
                        && call.name.endsWith("$nfrUi$replaceRootRotation")
                        && call.desc.equals(rootOperationDescriptor));
        requireReachableCallsite(ysm, "render", renderDescriptor, "YSM relative head pitch write",
                call -> call.name.startsWith("wrapOperation$")
                        && call.name.endsWith("$nfrUi$replaceHeadPitch")
                        && call.desc.equals(headOperationDescriptor));
        requireReachableCallsite(ysm, "render", renderDescriptor, "YSM relative head yaw write",
                call -> call.name.startsWith("wrapOperation$")
                        && call.name.endsWith("$nfrUi$replaceHeadYaw")
                        && call.desc.equals(headOperationDescriptor));
        printCallGraph(ysm, Set.of("render"));
    }

    private static void requireExactlyOne(ClassNode owner, String label,
                                          Predicate<MethodNode> predicate) {
        List<MethodNode> matches = owner.methods.stream().filter(predicate).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(owner.name + " must contain exactly one transformed "
                    + label + "; found=" + matches.stream().map(method ->
                    method.name + method.desc).toList() + ", observed=" + relevantMethodNames(owner));
        }
    }

    private static void requireExactMethod(ClassNode owner, String name) {
        List<MethodNode> matches = owner.methods.stream().filter(method -> method.name.equals(name)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(owner.name + " must contain exactly one " + name
                    + " helper, found=" + matches.size());
        }
    }

    private static void requireRenderCallsite(ClassNode owner, String targetMethod,
                                              String targetDescriptor,
                                              String callLabel,
                                              Predicate<MethodInsnNode> requiredCall) {
        List<MethodNode> targets = owner.methods.stream()
                .filter(method -> method.name.equals(targetMethod)
                        && method.desc.equals(targetDescriptor)).toList();
        if (targets.size() != 1) {
            List<String> observed = owner.methods.stream()
                    .filter(method -> method.name.equals(targetMethod))
                    .map(method -> method.name + method.desc).toList();
            throw new IllegalStateException(owner.name + " must have one " + targetMethod
                    + targetDescriptor + " target method in transformed bytecode, found="
                    + targets.size() + ", same-name overloads=" + observed);
        }
        int matches = 0;
        for (var instruction = targets.getFirst().instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && owner.name.equals(call.owner)
                    && requiredCall.test(call)) matches++;
        }
        if (matches != 1) {
            throw new IllegalStateException(owner.name + "." + targetMethod
                    + targetDescriptor + " must have exactly one transformed callsite for " + callLabel
                    + "; found=" + matches
                    + "; observed calls=" + calls(targets.getFirst()));
        }
    }

    private static void requireReachableCallsite(ClassNode owner, String targetMethod,
                                                 String targetDescriptor, String callLabel,
                                                 Predicate<MethodInsnNode> requiredCall) {
        List<MethodNode> roots = owner.methods.stream()
                .filter(method -> method.name.equals(targetMethod)
                        && method.desc.equals(targetDescriptor)).toList();
        if (roots.size() != 1) {
            throw new IllegalStateException(owner.name + " must have one " + targetMethod
                    + targetDescriptor + " graph root, found=" + roots.size());
        }
        Map<String, MethodNode> methodsBySignature = new HashMap<>();
        for (MethodNode method : owner.methods) {
            methodsBySignature.put(method.name + method.desc, method);
        }
        List<MethodNode> pending = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String rootSignature = roots.getFirst().name + roots.getFirst().desc;
        Map<String, String> predecessor = new HashMap<>();
        Map<String, String> incomingCall = new HashMap<>();
        pending.add(roots.getFirst());
        List<String> matchedCallSites = new ArrayList<>();
        String matchedCaller = null;
        while (!pending.isEmpty()) {
            MethodNode current = pending.removeLast();
            String signature = current.name + current.desc;
            if (!visited.add(signature)) continue;
            for (var instruction = current.instructions.getFirst(); instruction != null;
                 instruction = instruction.getNext()) {
                if (instruction instanceof InvokeDynamicInsnNode dynamicCall) {
                    if (!"java/lang/invoke/LambdaMetafactory".equals(dynamicCall.bsm.getOwner())
                            || !("metafactory".equals(dynamicCall.bsm.getName())
                            || "altMetafactory".equals(dynamicCall.bsm.getName()))
                            || dynamicCall.bsmArgs.length < 2
                            || !(dynamicCall.bsmArgs[1] instanceof Handle handle)
                            || !owner.name.equals(handle.getOwner())) continue;
                    MethodNode lambdaTarget = methodsBySignature.get(
                            handle.getName() + handle.getDesc());
                    if (lambdaTarget != null) {
                        String lambdaSignature = lambdaTarget.name + lambdaTarget.desc;
                        if (!rootSignature.equals(lambdaSignature)
                                && predecessor.putIfAbsent(lambdaSignature, signature) == null) {
                            incomingCall.put(lambdaSignature, "lambda "
                                    + handle.getName() + handle.getDesc());
                        }
                        pending.add(lambdaTarget);
                    }
                    continue;
                }
                if (!(instruction instanceof MethodInsnNode call)
                        || !owner.name.equals(call.owner)) continue;
                if (requiredCall.test(call)) {
                    matchedCallSites.add(current.name + current.desc + " -> "
                            + call.name + call.desc);
                    matchedCaller = signature;
                }
                MethodNode called = methodsBySignature.get(call.name + call.desc);
                if (called != null) {
                    String calledSignature = called.name + called.desc;
                    if (!rootSignature.equals(calledSignature)
                            && predecessor.putIfAbsent(calledSignature, signature) == null) {
                        incomingCall.put(calledSignature, "invoke " + call.name + call.desc);
                    }
                    pending.add(called);
                }
            }
        }
        if (matchedCallSites.size() != 1) {
            throw new IllegalStateException(owner.name + "." + targetMethod + targetDescriptor
                    + " call graph must reach exactly one " + callLabel + "; found="
                    + matchedCallSites.size() + ", callsites=" + matchedCallSites
                    + ", visitedMethodCount=" + visited.size());
        }
        List<String> callPath = new ArrayList<>();
        String cursor = matchedCaller;
        while (cursor != null && !rootSignature.equals(cursor)) {
            String edge = incomingCall.get(cursor);
            String parent = predecessor.get(cursor);
            if (edge == null || parent == null) {
                throw new IllegalStateException("Cannot reconstruct " + callLabel + " path from "
                        + rootSignature + " to " + matchedCaller + "; missing predecessor for " + cursor);
            }
            callPath.add(parent + " --" + edge + "--> " + cursor);
            cursor = parent;
        }
        java.util.Collections.reverse(callPath);
        callPath.add(matchedCallSites.getFirst());
        System.out.println("TRANSFORM_REACHABLE_PATH=" + owner.name + "." + rootSignature
                + "|" + callPath);
    }

    private static void printCallGraph(ClassNode owner, Set<String> rootMethods) {
        for (MethodNode method : owner.methods) {
            if (rootMethods.contains(method.name)
                    || method.name.endsWith("$nfrUi$renderWithSampledBodyPose")
                    || method.name.endsWith("$nfrUi$applyLongitudinalRoll")
                    || method.name.endsWith("$nfrUi$applyNonElytraLongitudinalRoll")
                    || method.name.endsWith("$nfrUi$replaceRootRotation")
                    || method.name.endsWith("$nfrUi$replaceHeadPitch")
                    || method.name.endsWith("$nfrUi$replaceHeadYaw")) {
                System.out.println("TRANSFORM_METHOD=" + method.name + method.desc
                        + "|calls=" + calls(method));
            }
        }
    }

    private static List<String> relevantMethodNames(ClassNode owner) {
        return owner.methods.stream().filter(method -> method.name.startsWith("wrapMethod$")
                        || method.name.startsWith("wrapOperation$")
                        || method.name.endsWith("$nfrUi$renderWithSampledBodyPose")
                        || method.name.endsWith("$nfrUi$replaceRootRotation")
                        || method.name.endsWith("$nfrUi$replaceHeadPitch")
                        || method.name.endsWith("$nfrUi$replaceHeadYaw"))
                .map(method -> method.name + method.desc).toList();
    }

    private static void printMethods(ClassNode owner, Set<String> names) {
        for (MethodNode method : owner.methods) {
            if (names.contains(method.name)) {
                System.out.println("TRANSFORM_TARGET_CANDIDATE=" + method.name + method.desc
                        + "|calls=" + calls(method));
            }
        }
    }

    private static List<String> calls(MethodNode method) {
        List<String> calls = new ArrayList<>();
        for (var instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call) {
                calls.add(call.owner + "." + call.name + call.desc);
            }
        }
        return calls;
    }

    private static Set<String> methodNames(ClassNode owner) {
        return owner.methods.stream().map(method -> method.name)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    /** Mirrors Foundation 0.19.11 LaunchHandler's pre-tweaker state setup, then stops before target launch. */
    private static void bootstrapLaunchState(Class<?> launchType, Class<?> launchLoaderType,
                                             Object launchLoader, Path gameDir, Path assetsDir)
            throws Throwable {
        Map<String, Object> blackboard = new HashMap<>();
        blackboard.put("TweakClasses", new java.util.ArrayList<String>());
        blackboard.put("ArgumentList", new java.util.ArrayList<String>());
        List<Object> tweaks = new ArrayList<>();
        blackboard.put("Tweaks", tweaks);
        launchType.getField("blackboard").set(null, blackboard);
        launchType.getField("minecraftHome").set(null, gameDir.toFile());
        launchType.getField("assetsDir").set(null, assetsDir.toFile());
        Thread.currentThread().setContextClassLoader((ClassLoader) launchLoader);

        ClassLoader loader = (ClassLoader) launchLoader;
        Class<?> actualClassLoader = Class.forName(
                "top.outlands.foundation.boot.ActualClassLoader", true, loader);
        Object holder = invoke(actualClassLoader.getMethod("getTransformerHolder"), null);
        Class<?> transformerDelegate = Class.forName("top.outlands.foundation.TransformerDelegate",
                true, loader);
        Method fillHolder = transformerDelegate.getDeclaredMethod("fillTransformerHolder",
                holder.getClass());
        fillHolder.setAccessible(true);
        invoke(fillHolder, null, holder);

        Class<?> explicitTransformer = Class.forName("top.outlands.foundation.IExplicitTransformer",
                true, loader);
        Method registerExplicit = transformerDelegate.getMethod("registerExplicitTransformer",
                explicitTransformer, String[].class);
        Object visitorTransformer = Class.forName(
                "top.outlands.foundation.transformer.ASMVisitorTransformer", true, loader)
                .getConstructor().newInstance();
        invoke(registerExplicit, null, visitorTransformer, (Object) new String[]{
                "org.objectweb.asm.FieldVisitor", "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor"});
        Object writerTransformer = Class.forName(
                "top.outlands.foundation.transformer.ASMClassWriterTransformer", true, loader)
                .getConstructor().newInstance();
        invoke(registerExplicit, null, writerTransformer,
                (Object) new String[]{"org.objectweb.asm.ClassWriter"});

        Method findClass = launchLoaderType.getMethod("findClass", String.class);
        for (String className : List.of("net.minecraft.launchwrapper.IClassTransformer",
                "net.minecraft.launchwrapper.ITweaker", "net.minecraft.launchwrapper.IClassNameTransformer",
                "org.objectweb.asm.FieldVisitor", "org.objectweb.asm.ClassVisitor",
                "org.objectweb.asm.MethodVisitor", "org.objectweb.asm.ClassWriter")) {
            invoke(findClass, launchLoader, className);
        }
        invoke(actualClassLoader.getMethod("addTransformerExclusion", String.class),
                launchLoader, "org.objectweb.asm.");
        System.out.println("FOUNDATION_LAUNCH_STATE=initialized-from-0.19.11-LaunchHandler-prefix");
    }

    private static void stage(String name) {
        System.out.println("PROBE_STAGE=" + name);
    }

    private static void requireRegularFile(Path file, String label) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(label + " does not exist: " + file);
        }
    }

    private static void reportCodeSource(String name, Class<?> type) {
        System.out.println("CODESOURCE=" + name + "|" + codeSource(type));
    }

    private static String codeSource(Class<?> type) {
        if (type.getProtectionDomain() == null || type.getProtectionDomain().getCodeSource() == null) {
            return "unavailable";
        }
        return String.valueOf(type.getProtectionDomain().getCodeSource().getLocation());
    }

    private static void requireDefinedBy(Class<?> type, ClassLoader expected) {
        if (type.getClassLoader() != expected) {
            throw new IllegalStateException(type.getName() + " was defined by "
                    + type.getClassLoader() + ", expected the actual LaunchClassLoader");
        }
    }

    private static void requireCodeSource(Class<?> type, Path expected) throws Exception {
        if (type.getProtectionDomain() == null || type.getProtectionDomain().getCodeSource() == null) {
            throw new IllegalStateException(type.getName() + " has no CodeSource; expected " + expected);
        }
        URL sourceUrl = type.getProtectionDomain().getCodeSource().getLocation();
        Path expectedJar = expected.toRealPath();
        if (sourceUrl == null) {
            throw new IllegalStateException(type.getName() + " has a CodeSource without a location; expected "
                    + expectedJar);
        }
        System.out.println("COREMOD_CODE_SOURCE_URL=" + type.getName() + "|rawSourceUrl=" + sourceUrl
                + "|expectedJar=" + expectedJar);

        URL archiveUrl;
        if ("file".equalsIgnoreCase(sourceUrl.getProtocol())) {
            archiveUrl = sourceUrl;
        } else if ("jar".equalsIgnoreCase(sourceUrl.getProtocol())) {
            URLConnection connection = sourceUrl.openConnection();
            if (!(connection instanceof JarURLConnection jarConnection)) {
                throw new IllegalStateException(type.getName() + " has a jar: CodeSource that is not backed by "
                        + "JarURLConnection: " + sourceUrl);
            }
            archiveUrl = jarConnection.getJarFileURL();
        } else {
            throw new IllegalStateException(type.getName() + " has an unsupported CodeSource protocol: "
                    + sourceUrl.getProtocol() + " | sourceUrl=" + sourceUrl + " | expected=" + expectedJar);
        }
        if (!"file".equalsIgnoreCase(archiveUrl.getProtocol())) {
            throw new IllegalStateException(type.getName() + " CodeSource does not resolve to a local JAR file: "
                    + "sourceUrl=" + sourceUrl + " | archiveUrl=" + archiveUrl + " | expected=" + expectedJar);
        }

        Path actualJar;
        try {
            actualJar = Path.of(archiveUrl.toURI()).toRealPath();
        } catch (IOException | URISyntaxException exception) {
            throw new IllegalStateException(type.getName() + " CodeSource could not be resolved to a canonical "
                    + "JAR path: sourceUrl=" + sourceUrl + " | archiveUrl=" + archiveUrl
                    + " | expected=" + expectedJar, exception);
        }
        System.out.println("COREMOD_CODE_SOURCE_CANONICAL=" + type.getName() + "|sourceUrl=" + sourceUrl
                + "|canonicalSource=" + actualJar + "|expectedJar=" + expectedJar);
        if (!actualJar.equals(expectedJar)) {
            throw new IllegalStateException(type.getName() + " was loaded from " + actualJar
                    + ", expected staged JAR " + expectedJar + " | sourceUrl=" + sourceUrl);
        }
    }

    private static Object invoke(Method method, Object receiver, Object... arguments)
            throws Throwable {
        try {
            return method.invoke(receiver, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static Object instantiate(Constructor<?> constructor) throws Throwable {
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static boolean readYsmPoseConfigEnabled(ClassLoader actualLoader) throws Throwable {
        // The host deliberately keeps UIE classes inside LaunchClassLoader. Loading the same class
        // through the probe's system loader would test a different config singleton and miss the
        // early Mixin gate's persisted value.
        Class<?> configType = Class.forName("neofontrender.addons.compat.YsmPoseConfig", true, actualLoader);
        requireDefinedBy(configType, actualLoader);
        return (boolean) invoke(configType.getMethod("enabled"), null);
    }
}
