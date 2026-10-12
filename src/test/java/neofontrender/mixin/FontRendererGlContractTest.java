package neofontrender.mixin;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.7.10 exposes alpha through {@code GL11.glEnable(GL11.GL_ALPHA_TEST)} rather than a
 * {@code GlStateManager} cache, so the mixin must synchronize the driver state before any
 * replacement-backend glyph draw can happen.
 */
class FontRendererGlContractTest {
    private static final String GL11_OWNER = "org/lwjgl/opengl/GL11";
    private static final String BACKEND = "neofontrender/core/font/backend/TextRenderBackend";

    @Test
    void enablesDriverAlphaBeforeAnyBackendDrawStringPath() throws Exception {
        int[] methodCallIndex = {0};
        int[] enableAlphaIndex = {-1};
        int[] firstBackendDrawIndex = {-1};
        visit("sfr$onDrawString", new MethodVisitor(Opcodes.ASM9) {
            private Integer pushedInt;

            @Override
            public void visitInsn(int opcode) {
                pushedInt = opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5
                        ? opcode - Opcodes.ICONST_0 : null;
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                pushedInt = opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH ? operand : null;
            }

            @Override
            public void visitLdcInsn(Object value) {
                pushedInt = value instanceof Integer ? (Integer) value : null;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                        boolean isInterface) {
                int index = methodCallIndex[0]++;
                if (GL11_OWNER.equals(owner) && "glEnable".equals(name)
                        && pushedInt != null && pushedInt == GL11.GL_ALPHA_TEST) {
                    enableAlphaIndex[0] = index;
                }
                if (BACKEND.equals(owner)
                        && ("renderFormatted".equals(name) || "renderFormattedWithShadow".equals(name))) {
                    firstBackendDrawIndex[0] = index;
                }
                pushedInt = null;
            }
        });

        assertTrue(enableAlphaIndex[0] >= 0, "drawString hook must enable alpha");
        assertTrue(firstBackendDrawIndex[0] >= 0,
                "expected the replacement backend draw path");
        assertTrue(enableAlphaIndex[0] < firstBackendDrawIndex[0],
                "driver alpha state must be synchronized before a backend draw can return");
    }

    @Test
    void preprocessedPathAlsoSynchronizesDriverAlpha() throws Exception {
        int[] enableAlphaIndex = {-1};
        visit("sfr$drawPreprocessedText", new MethodVisitor(Opcodes.ASM9) {
            private Integer pushedInt;

            @Override
            public void visitInsn(int opcode) {
                pushedInt = opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5
                        ? opcode - Opcodes.ICONST_0 : null;
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                pushedInt = opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH ? operand : null;
            }

            @Override
            public void visitLdcInsn(Object value) {
                pushedInt = value instanceof Integer ? (Integer) value : null;
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                        boolean isInterface) {
                if (GL11_OWNER.equals(owner) && "glEnable".equals(name)
                        && pushedInt != null && pushedInt == GL11.GL_ALPHA_TEST) {
                    enableAlphaIndex[0] = 0;
                }
                pushedInt = null;
            }
        });

        assertTrue(enableAlphaIndex[0] >= 0,
                "the preprocessed modern-text path must enable alpha as well");
    }

    private static void visit(String selectedMethod, MethodVisitor visitor) throws Exception {
        InputStream stream = MixinFontRenderer.class.getResourceAsStream(
                "/neofontrender/mixin/MixinFontRenderer.class");
        assertNotNull(stream);
        try (InputStream input = stream) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    return selectedMethod.equals(name) ? visitor : null;
                }
            }, 0);
        }
    }
}
