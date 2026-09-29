package neofontrender.addons.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.util.List;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Checks actual HEI bytecode without loading its Minecraft-dependent classes. */
class HeiBinaryContractTest {
    @Test
    void tooltipLifecycleAndPanelRedirectsMatchInstalledHei() throws Exception {
        String path = System.getProperty("nfr.test.heiJar", "");
        assumeTrue(!path.isEmpty(), "Supply -PheiCompatJar to verify an installed HEI JAR");
        try (JarFile jar = new JarFile(path)) {
            ClassNode target = read(jar.getInputStream(jar.getJarEntry("mezz/jei/gui/TooltipRenderer.class")));
            ClassNode mixin = read(getClass().getClassLoader().getResourceAsStream(
                    "neofontrender/addons/mixin/compat/MixinHeiTooltipRenderer.class"));
            int heads = 0, returns = 0, redirects = 0;
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    boolean inject = annotation.desc.endsWith("/Inject;");
                    boolean redirect = annotation.desc.endsWith("/Redirect;");
                    if (!inject && !redirect) continue;
                    List<String> selectors = value(annotation, "method");
                    MethodNode matched = target.methods.stream()
                            .filter(m -> selectors.contains(m.name + m.desc)).findFirst().orElse(null);
                    // Only one of the legacy/current lifecycle pairs should apply.
                    if (matched == null && inject) continue;
                    assertNotNull(matched, "No HEI method matches " + selectors);
                    if (inject) {
                        List<AnnotationNode> locations = value(annotation, "at");
                        String location = value(locations.getFirst(), "value");
                        if (location.equals("HEAD")) heads++;
                        if (location.equals("RETURN")) returns++;
                        Type[] parameters = Type.getArgumentTypes(handler.desc);
                        String callback = Type.getReturnType(matched.desc).equals(Type.VOID_TYPE)
                                ? "CallbackInfo" : "CallbackInfoReturnable";
                        assertEquals("org.spongepowered.asm.mixin.injection.callback." + callback,
                                parameters[parameters.length - 1].getClassName());
                        assertFalse(cancelsCallback(handler),
                                "The hook must preserve HEI's returned bounds and rendering");
                    } else {
                        AnnotationNode at = value(annotation, "at");
                        String invocation = value(at, "target");
                        assertTrue(hasInvocation(matched, invocation), "Missing redirect target " + invocation);
                        redirects++;
                    }
                }
            }
            assertEquals(1, heads, "HEI context must begin before RenderTooltipEvent.Pre");
            assertEquals(1, returns, "HEI context must end on every return, including cancelled Pre");
            assertEquals(3, redirects, "Both screen limits and the panel background must match HEI");
        }
    }

    @Test
    void scrollbarDrawAndAreaAccessorMatchInstalledHei() throws Exception {
        String path = System.getProperty("nfr.test.heiJar", "");
        assumeTrue(!path.isEmpty(), "Supply -PheiCompatJar to verify an installed HEI JAR");
        try (JarFile jar = new JarFile(path)) {
            ClassNode scrollbar = read(jar.getInputStream(jar.getJarEntry("mezz/jei/gui/elements/ScrollBar.class")));
            assertTrue(scrollbar.methods.stream().anyMatch(m -> m.name.equals("draw")
                    && m.desc.equals("(Lnet/minecraft/client/Minecraft;IIF)V")));
            assertTrue(scrollbar.methods.stream().anyMatch(m -> m.name.equals("getArea")
                    && m.desc.equals("()Ljava/awt/Rectangle;")));
            assertTrue(scrollbar.methods.stream().anyMatch(m -> m.name.equals("isDragging")
                    && m.desc.equals("()Z")));
        }
    }

    @Test
    void pinnedGridHoverHooksMatchInstalledHei() throws Exception {
        String path = System.getProperty("nfr.test.heiJar", "");
        assumeTrue(!path.isEmpty(), "Supply -PheiCompatJar to verify an installed HEI JAR");
        try (JarFile jar = new JarFile(path)) {
            ClassNode preview = read(jar.getInputStream(jar.getJarEntry(
                    "mezz/jei/gui/ingredients/IngredientListPreview.class")));
            ClassNode mixin = read(getClass().getClassLoader().getResourceAsStream(
                    "neofontrender/addons/mixin/compat/MixinHeiIngredientPreviewHover.class"));
            int hooks = 0;
            for (MethodNode handler : mixin.methods) {
                if (handler.visibleAnnotations == null) continue;
                for (AnnotationNode annotation : handler.visibleAnnotations) {
                    boolean inject = annotation.desc.endsWith("/Inject;");
                    boolean redirect = annotation.desc.endsWith("/Redirect;");
                    boolean modify = annotation.desc.endsWith("/ModifyVariable;");
                    if (!inject && !redirect && !modify) continue;
                    List<String> selectors = value(annotation, "method");
                    MethodNode target = preview.methods.stream()
                            .filter(m -> selectors.contains(m.name + m.desc)).findFirst().orElseThrow();
                    AnnotationNode at = inject ? ((List<AnnotationNode>) value(annotation, "at")).getFirst()
                            : value(annotation, "at");
                    if ("INVOKE".equals(value(at, "value"))) {
                        assertTrue(hasInvocation(target, value(at, "target")), handler.name);
                    }
                    if (modify) {
                        assertEquals("(Ljava/awt/Point;)Ljava/awt/Point;", handler.desc);
                        assertEquals("STORE", value(at, "value"));
                        int originStores = 0;
                        for (AbstractInsnNode instruction : target.instructions) {
                            if (instruction instanceof MethodInsnNode call && call.name.equals("getRenderOrigin")
                                    && call.desc.equals("()Ljava/awt/Point;")) {
                                AbstractInsnNode next = instruction.getNext();
                                while (next != null && next.getOpcode() < 0) next = next.getNext();
                                assertNotNull(next);
                                assertEquals(org.objectweb.asm.Opcodes.ASTORE, next.getOpcode());
                                originStores++;
                            }
                        }
                        assertEquals(1, originStores, "Capture the grid origin before HEI's early returns");
                    }
                    hooks++;
                }
            }
            assertEquals(5, hooks, "Frame start, origin, highlight, fade-out and scroll invalidation");
        }
    }

    private static boolean cancelsCallback(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && (call.name.equals("setReturnValue") || call.name.equals("cancel"))) return true;
        }
        return false;
    }

    private static boolean hasInvocation(MethodNode method, String target) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && target.equals("L" + call.owner + ";" + call.name + call.desc)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(AnnotationNode annotation, String name) {
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return (T) annotation.values.get(i + 1);
        }
        throw new AssertionError("Missing annotation value " + name);
    }

    private static ClassNode read(InputStream stream) throws Exception {
        assertNotNull(stream);
        try (InputStream input = stream) {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }
}
