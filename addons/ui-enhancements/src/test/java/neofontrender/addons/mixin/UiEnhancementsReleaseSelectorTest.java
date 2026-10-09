package neofontrender.addons.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEnhancementsReleaseSelectorTest {
    private static final String MIXIN_CLASS =
            "neofontrender/addons/mixin/MixinRenderPlayerFlightRoll";
    private static final String RENDER_PLAYER_CLASS =
            "net/minecraft/client/renderer/entity/RenderPlayer";
    private static final String MAPPED_DO_RENDER =
            "func_76986_a(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V";
    private static final String MCP_DO_RENDER =
            "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V";
    private static final String WRAP_METHOD =
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;";
    private static final String REF_MAP =
            "mixins.neofontrender_ui_enhancements-refmap.json";
    private static final String EXPECTED_CLEANROOM_MCP_JAR_SHA256 =
            "F833BB8552A51103F09AED3410030E7818A73359F0878DB6208A451B290D8B71";
    private static final String EXPECTED_PROFILE_ID = "QuE-Eng_Step-B0";

    @Test
    @Tag("release-artifact")
    void packagedDevAndReleaseSelectorsEachMatchExactlyOneMethodInTheirTargetNamespace()
            throws Exception {
        Path devJar = configuredPath("uie.devJar");
        Path releaseJar = configuredPath("uie.releaseJar");
        Path cleanroomMcpJar = configuredPath("uie.cleanroomMcpJar");
        Path cleanroomClientRoot = configuredPath("uie.cleanroomClientRoot");
        Path cleanroomProfileJson = configuredPath("uie.cleanroomProfileJson");
        Path mcpSrg = configuredPath("uie.mcpSrg");
        assertEquals(EXPECTED_CLEANROOM_MCP_JAR_SHA256, sha256(cleanroomMcpJar));

        Selector dev = selector(devJar);
        Selector release = selector(releaseJar);
        assertOptionalYsmMixinPackaged(devJar);
        assertOptionalYsmMixinPackaged(releaseJar);
        assertEquals(dev, release,
                "The remapped JAR must retain the exact dual target contract");
        assertEquals(Set.of(MCP_DO_RENDER, MAPPED_DO_RENDER), dev.methodSelectors());
        assertFalse(dev.remap(),
                "The two explicit names bypass the incomplete release refmap mapping");
        assertEquals(1, dev.require());
        assertEquals(1, dev.expect());
        assertEquals(1, dev.allow());

        assertTargetSelectorsMatchExactlyOnce(cleanroomMcpJar,
                dev.methodSelectors(), MCP_DO_RENDER);
        assertSrgMappingTargetsTheExactMcpMethod(mcpSrg);
        assertCleanroomClientInput(cleanroomClientRoot, cleanroomProfileJson);
        assertFlightApi10(releaseJar);

        assertRefmapRetainsValidAdjacentRootMapping(devJar);
        assertRefmapRetainsValidAdjacentRootMapping(releaseJar);
        assertOptionalWrapperRefmapEntryIsCorrect(devJar);
        assertOptionalWrapperRefmapEntryIsCorrect(releaseJar);
    }

    @Test
    @Tag("release-artifact")
    void releaseJarDoesNotBundleOptionalYsmMixinExtrasOrNightConfigLibraries()
            throws Exception {
        Path releaseJar = configuredPath("uie.releaseJar");
        try (JarFile archive = new JarFile(releaseJar.toFile())) {
            List<String> bundledCopies = archive.stream().map(JarEntry::getName)
                    .filter(name -> name.startsWith("com/elfmcys/yesstevemodel/")
                            || name.startsWith("com/llamalad7/mixinextras/")
                            || name.startsWith("org/spongepowered/asm/")
                            || name.startsWith("neofontrender/internal/nightconfig/")
                            || name.startsWith("com/electronwill/nightconfig/"))
                    .sorted().toList();
            assertTrue(bundledCopies.isEmpty(),
                    "UIE must consume these host libraries without bundling copies: "
                            + bundledCopies);
        }
    }

    private static void assertSrgMappingTargetsTheExactMcpMethod(Path mappings)
            throws Exception {
        List<String> matchedMappings = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(mappings,
                StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("MD: ")) continue;
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 5
                        && parts[1].equals("net/minecraft/client/renderer/entity/RenderPlayer/doRender")
                        && parts[2].equals("(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V")
                        && parts[3].equals("net/minecraft/client/renderer/entity/RenderPlayer/func_76986_a")
                        && parts[4].equals("(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V")) {
                    matchedMappings.add(line);
                }
            }
        }
        assertEquals(1, matchedMappings.size(),
                "The exact MCP selector must map once to its production SRG selector");
    }

    private static void assertCleanroomClientInput(Path clientRoot, Path profileJson)
            throws Exception {
        Path versionJson = profileJson.toAbsolutePath().normalize();
        try (InputStream input = Files.newInputStream(versionJson);
             InputStreamReader reader = new InputStreamReader(input,
                     StandardCharsets.UTF_8)) {
            JsonObject version = new JsonParser().parse(reader).getAsJsonObject();
            assertEquals(EXPECTED_PROFILE_ID, version.get("id").getAsString());
            assertEquals("top.outlands.foundation.boot.Foundation",
                    version.get("mainClass").getAsString());
            assertEquals(EXPECTED_PROFILE_ID, version.get("jar").getAsString());
            assertFalse(version.has("inheritsFrom"),
                    "The actual profile is resolved directly and has no inherited version");
            assertEquals(167, version.getAsJsonArray("libraries").size());
            Set<String> libraries = new HashSet<>();
            version.getAsJsonArray("libraries").forEach(library ->
                    libraries.add(library.getAsJsonObject().get("name").getAsString()));
            assertTrue(libraries.contains("com.cleanroommc:cleanroom:0.6.8-alpha"));
            assertTrue(libraries.contains("top.outlands:foundation:0.19.11"));
            assertTrue(libraries.contains("com.cleanroommc:cleanmix:0.7.0"));
            assertTrue(libraries.contains("com.cleanroommc:mixinextras-common:0.5.5"));
            assertTrue(libraries.contains("com.cleanroommc:lwjglxx:1.1.21"));
        }
        Path versionDirectory = clientRoot.resolve("versions").resolve(EXPECTED_PROFILE_ID);
        assertEquals(versionDirectory.resolve(EXPECTED_PROFILE_ID + ".json"), versionJson,
                "The JSON must be the configured profile under the selected client root");
        Path clientJar = versionDirectory.resolve(EXPECTED_PROFILE_ID + ".jar");
        assertTrue(Files.isRegularFile(clientJar), "Missing profile client JAR: " + clientJar);
        assertTrue(Files.size(clientJar) > 0L, "Empty profile client JAR: " + clientJar);
        try (JarFile rawClient = new JarFile(clientJar.toFile())) {
            assertNull(rawClient.getJarEntry(RENDER_PLAYER_CLASS + ".class"),
                    "The actual launcher input is obfuscated; production mapping must be applied");
        }
    }

    private static void assertFlightApi10(Path releaseJar) throws Exception {
        try (JarFile archive = new JarFile(releaseJar.toFile())) {
            String apiPath = "neofontrender/addons/api/flight/FlightApi.class";
            JarEntry apiEntry = archive.getJarEntry(apiPath);
            assertNotNull(apiEntry, "Missing FlightApi API10 class in " + releaseJar);
            ClassNode api = new ClassNode(Opcodes.ASM9);
            try (InputStream input = archive.getInputStream(apiEntry)) {
                new ClassReader(input).accept(api,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            List<Object> apiVersions = api.fields.stream()
                    .filter(field -> field.name.equals("API_VERSION") && field.desc.equals("I"))
                    .map(field -> field.value).toList();
            assertEquals(List.of(10), apiVersions,
                    "The installable artifact must publish API10, not only update its sources");

            String posePath = "neofontrender/addons/api/flight/FlightBodyPose.class";
            JarEntry poseEntry = archive.getJarEntry(posePath);
            assertNotNull(poseEntry, "Missing FlightBodyPose in " + releaseJar);
            ClassNode pose = new ClassNode(Opcodes.ASM9);
            try (InputStream input = archive.getInputStream(poseEntry)) {
                new ClassReader(input).accept(pose,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            String attitude = "Lneofontrender/addons/api/flight/FlightAttitude;";
            String owner = "Lneofontrender/addons/api/flight/FlightBodyPose;";
            assertTrue(pose.methods.stream().anyMatch(method -> method.name.equals("<init>")
                            && method.desc.equals("(" + attitude + ")V")),
                    "The original one-argument constructor ABI must remain in the release artifact");
            assertTrue(pose.methods.stream().anyMatch(method -> method.name.equals("modelUpY")
                            && method.desc.equals("(" + attitude + ")" + owner)),
                    "API10 must expose the explicit model-up-Y factory");
            assertTrue(pose.methods.stream().anyMatch(method -> method.name.equals("modelUpY")
                            && method.desc.equals("(" + attitude + attitude + ")" + owner)),
                    "API10 must expose the body-plus-head model-up-Y factory");
        }
    }

    private static Selector selector(Path jar) throws Exception {
        try (JarFile archive = new JarFile(jar.toFile())) {
            JarEntry entry = archive.getJarEntry(MIXIN_CLASS + ".class");
            assertNotNull(entry, "Missing mixin class in artifact: " + jar);
            try (InputStream input = archive.getInputStream(entry)) {
                SelectorVisitor visitor = new SelectorVisitor();
                new ClassReader(input).accept(visitor, ClassReader.SKIP_CODE
                        | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                assertNotNull(visitor.selector,
                        "Missing @WrapMethod in artifact: " + jar);
                return visitor.selector;
            }
        }
    }

    private static void assertOptionalYsmMixinPackaged(Path artifact) throws Exception {
        String mixinConfigPath = "mixins.neofontrender_ui_enhancements.json";
        String mixinClassPath = "neofontrender/addons/mixin/MixinYsmPlayerBodyPose.class";
        try (JarFile archive = new JarFile(artifact.toFile())) {
            JarEntry configEntry = archive.getJarEntry(mixinConfigPath);
            assertNotNull(configEntry, "Missing optional YSM mixin config in " + artifact);
            JsonObject config;
            try (InputStream input = archive.getInputStream(configEntry);
                 InputStreamReader reader = new InputStreamReader(input,
                         StandardCharsets.UTF_8)) {
                config = new JsonParser().parse(reader).getAsJsonObject();
            }
            assertEquals("@env(DEFAULT)", config.get("target").getAsString());
            JsonArray clientMixins = config.getAsJsonArray("client");
            assertTrue(clientMixins != null && clientMixins.asList().stream()
                            .anyMatch(entry -> entry.isJsonPrimitive()
                                    && "MixinYsmPlayerBodyPose".equals(entry.getAsString())),
                    "The optional YSM mixin must be registered client-side in " + artifact);
            assertNotNull(archive.getJarEntry(mixinClassPath),
                    "Missing optional YSM mixin class in " + artifact);
        }
    }

    private static void assertTargetSelectorsMatchExactlyOnce(Path targetJar,
                                                               Set<String> selectors,
                                                               String expectedSelector)
            throws Exception {
        List<String> targetMethods = new ArrayList<>();
        try (JarFile archive = new JarFile(targetJar.toFile())) {
            JarEntry entry = archive.getJarEntry(RENDER_PLAYER_CLASS + ".class");
            assertNotNull(entry, "Missing RenderPlayer class in " + targetJar);
            try (InputStream input = archive.getInputStream(entry)) {
                new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name,
                                                     String descriptor, String signature,
                                                     String[] exceptions) {
                        String actual = name + descriptor;
                        if (name.equals("doRender") || name.equals("func_76986_a")) {
                            targetMethods.add(actual);
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
        List<String> matches = selectors.stream().filter(targetMethods::contains).sorted().toList();
        assertEquals(List.of(expectedSelector), matches,
                "Exact target selectors in " + targetJar
                        + " must resolve to exactly one method; unrelated overloads are allowed");
    }

    private static void assertRefmapRetainsValidAdjacentRootMapping(Path artifact)
            throws Exception {
        try (JarFile archive = new JarFile(artifact.toFile())) {
            JarEntry entry = archive.getJarEntry(REF_MAP);
            assertNotNull(entry, "Missing refmap in artifact: " + artifact);
            try (InputStream input = archive.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(input,
                         StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                String selector = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V";
                String mapped = refmapValue(root, selector);
                assertEquals("Lnet/minecraft/client/renderer/entity/RenderPlayer;"
                                + "func_77043_a(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V",
                        mapped, "The adjacent Inject selector still needs its valid refmap entry");
            }
        }
    }

    private static void assertOptionalWrapperRefmapEntryIsCorrect(Path artifact)
            throws Exception {
        try (JarFile archive = new JarFile(artifact.toFile())) {
            JarEntry entry = archive.getJarEntry(REF_MAP);
            assertNotNull(entry);
            try (InputStream input = archive.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(input,
                         StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                String mapped = refmapValue(root, MCP_DO_RENDER);
                assertTrue(mapped == null || mapped.equals(
                                "Lnet/minecraft/client/renderer/entity/RenderPlayer;"
                                        + MAPPED_DO_RENDER),
                        "If present, the optional refmap entry must point to the exact SRG target");
            }
        }
    }

    private static String refmapValue(JsonObject root, String selector) {
        String mixinKey = MIXIN_CLASS;
        JsonElement mappings = root.get("mappings");
        if (mappings != null && mappings.isJsonObject()) {
            JsonObject direct = mappings.getAsJsonObject().getAsJsonObject(mixinKey);
            if (direct != null && direct.has(selector)) {
                return direct.get(selector).getAsString();
            }
        }
        JsonElement data = root.get("data");
        if (data != null && data.isJsonObject()) {
            JsonElement searge = data.getAsJsonObject().get("searge");
            if (searge != null && searge.isJsonObject()) {
                JsonObject perMixin = searge.getAsJsonObject().getAsJsonObject(mixinKey);
                if (perMixin != null && perMixin.has(selector)) {
                    return perMixin.get(selector).getAsString();
                }
            }
        }
        return null;
    }

    private static Path configuredPath(String property) {
        String configured = System.getProperty(property);
        assertTrue(configured != null && !configured.isBlank(),
                "The dedicated release-selector task must set -D" + property);
        return Path.of(configured).toAbsolutePath().normalize();
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    private record Selector(Set<String> methodSelectors, boolean remap,
                            int require, int expect, int allow) {}

    private static final class SelectorVisitor extends ClassVisitor {
        private Selector selector;

        private SelectorVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor,
                                                         boolean visible) {
                    if (!WRAP_METHOD.equals(annotationDescriptor)) return null;
                    SelectorBuilder builder = new SelectorBuilder();
                    return builder.annotation(value -> SelectorVisitor.this.selector = value);
                }
            };
        }
    }

    @FunctionalInterface
    private interface SelectorConsumer {
        void accept(Selector selector);
    }

    private static final class SelectorBuilder extends AnnotationVisitor {
        private final Set<String> methodSelectors = new HashSet<>();
        private boolean remap = true;
        private int require = -1;
        private int expect = -1;
        private int allow = -1;

        private SelectorBuilder() {
            super(Opcodes.ASM9);
        }

        private AnnotationVisitor annotation(SelectorConsumer consumer) {
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String name, Object value) {
                    switch (name) {
                        case "remap" -> remap = (boolean) value;
                        case "require" -> require = (int) value;
                        case "expect" -> expect = (int) value;
                        case "allow" -> allow = (int) value;
                        default -> { }
                    }
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (!name.equals("method")) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String ignored, Object value) {
                            methodSelectors.add((String) value);
                        }
                    };
                }

                @Override
                public void visitEnd() {
                    consumer.accept(new Selector(Set.copyOf(methodSelectors), remap,
                            require, expect, allow));
                }
            };
        }
    }
}
