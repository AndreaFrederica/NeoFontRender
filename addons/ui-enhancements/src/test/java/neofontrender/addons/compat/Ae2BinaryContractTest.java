package neofontrender.addons.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Validates the compiled bridge against a real AE2 development artifact without loading Minecraft. */
class Ae2BinaryContractTest {
    @Test
    void bridgeCallsMatchRealAe2Api() throws Exception {
        String path = System.getProperty("nfr.test.ae2Jar", "");
        assumeTrue(!path.isEmpty(), "Supply -Pae2CompatJar with the AE2 development JAR");
        try (JarFile jar = new JarFile(path);
             var stream = getClass().getClassLoader().getResourceAsStream(
                     "neofontrender/addons/tooltips/NfrAe2TooltipApi.class")) {
            assertNotNull(stream);
            ClassNode bridge = new ClassNode();
            new ClassReader(stream).accept(bridge, 0);
            int checked = 0;
            for (var method : bridge.methods) {
                for (var instruction : method.instructions) {
                    if (!(instruction instanceof MethodInsnNode call) || !call.owner.startsWith("ae2/")) continue;
                    var entry = jar.getJarEntry(call.owner + ".class");
                    assertNotNull(entry, call.owner);
                    ClassNode target = new ClassNode();
                    try (var input = jar.getInputStream(entry)) {
                        new ClassReader(input).accept(target, ClassReader.SKIP_CODE);
                    }
                    assertEquals(call.itf, (target.access & Opcodes.ACC_INTERFACE) != 0, call.owner);
                    var actual = target.methods.stream().filter(m -> m.name.equals(call.name)
                            && m.desc.equals(call.desc)).findFirst().orElse(null);
                    assertNotNull(actual, call.owner + "." + call.name + call.desc);
                    assertEquals(call.getOpcode() == Opcodes.INVOKESTATIC,
                            (actual.access & Opcodes.ACC_STATIC) != 0, call.name);
                    checked++;
                }
            }
            assertTrue(checked >= 10, "The bridge's item, fluid and component accessors must be checked");
            ClassNode renderer = new ClassNode();
            try (var input = jar.getInputStream(jar.getJarEntry("ae2/client/gui/StackTooltipRenderer.class"))) {
                new ClassReader(input).accept(renderer, ClassReader.SKIP_CODE);
            }
            assertTrue(renderer.methods.stream().anyMatch(m -> m.name.equals("isReservedTooltipLine")
                    && m.desc.equals("(Ljava/lang/String;)Z") && (m.access & Opcodes.ACC_STATIC) != 0));
        }
    }
}
