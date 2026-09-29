package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmicGlStateContractTest {
    private static final String RENDERER =
            "neofontrender/core/font/cosmic/CosmicTextRenderer";
    private static final String SDF =
            "neofontrender/core/font/cosmic/CosmicSdfPipeline";

    @Test
    void rgbaRendererSelectsAndRestoresTextureUnits() throws Exception {
        Set<String> constructor = calls(RENDERER + "$PremultipliedBlendState", "<init>");
        Set<String> close = calls(RENDERER + "$PremultipliedBlendState", "close");
        assertTrue(constructor.contains(
                RENDERER + "$PremultipliedBlendState.selectTextureUnit"));
        assertTrue(close.contains(
                RENDERER + "$PremultipliedBlendState.restoreTextureUnit"));
        assertTrue(close.contains(
                RENDERER + "$PremultipliedBlendState.selectTextureUnit"));
    }

    @Test
    void sdfRendererSelectsAndRestoresTextureUnits() throws Exception {
        Set<String> capture = calls(SDF + "$State", "capture");
        Set<String> close = calls(SDF + "$State", "close");
        assertTrue(capture.contains(SDF + "$State.selectTextureUnit"));
        assertTrue(close.contains(SDF + "$State.restoreTextureUnit"));
        assertTrue(close.contains(SDF + "$State.selectTextureUnit"));
    }

    private static Set<String> calls(String owner, String selectedMethod) throws Exception {
        InputStream stream = CosmicGlStateContractTest.class.getResourceAsStream(
                "/" + owner + ".class");
        assertNotNull(stream);
        Set<String> calls = new HashSet<>();
        try (InputStream input = stream) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!selectedMethod.equals(name)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String methodOwner, String name,
                                                    String descriptor, boolean isInterface) {
                            calls.add(methodOwner + "." + name);
                        }
                    };
                }
            }, 0);
        }
        return calls;
    }
}
