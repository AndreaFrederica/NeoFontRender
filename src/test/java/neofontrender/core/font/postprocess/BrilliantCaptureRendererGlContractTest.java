package neofontrender.core.font.postprocess;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bytecode contract for the 1.7.10 renderer: there is no {@code GlStateManager} cache to keep in
 * sync, so the capture/composite passes must talk to the OpenGL driver through {@code GL11} and
 * restore everything they mutate.
 */
class BrilliantCaptureRendererGlContractTest {
    private static final String OWNER =
            "neofontrender/core/font/postprocess/BrilliantCaptureRenderer";
    private static final String GL11_OWNER = "org/lwjgl/opengl/GL11";

    @Test
    void captureAndParticlesUseTheSameCallerStateBoundary() throws Exception {
        Set<String> capturedCalls = callsIn("renderCaptured");
        Set<String> particleCalls = callsIn("renderParticles");
        String stateCapture = OWNER + "$CallerGlState.capture";
        String matrixCapture = OWNER + "$CallerMatrices.capture";

        assertTrue(capturedCalls.contains(stateCapture));
        assertTrue(capturedCalls.contains(matrixCapture));
        assertTrue(particleCalls.contains(stateCapture));
        assertTrue(particleCalls.contains(matrixCapture));
        // Particles composite on top of the caller's own target, so they must not bind an
        // off-screen framebuffer or change the viewport the way the capture pass does.
        assertFalse(particleCalls.contains("org/lwjgl/opengl/GL30.glBindFramebuffer"));
        assertFalse(particleCalls.contains(GL11_OWNER + ".glViewport"));
    }

    @Test
    void callerStateRestoresDriverStateAfterTheAttributePop() throws Exception {
        Set<String> calls = callsInNested("CallerGlState", "close");
        assertTrue(calls.contains(GL11_OWNER + ".glPopAttrib"));
        assertTrue(calls.contains(OWNER + "$CallerGlState.restoreFramebufferAndViewport"));
        assertTrue(calls.contains(OWNER + "$CallerGlState.restoreTextureUnits"));
        assertTrue(calls.contains("org/lwjgl/opengl/GL20.glUseProgram"));
        assertTrue(calls.contains("org/lwjgl/opengl/GL14.glBlendFuncSeparate"));
        assertTrue(calls.contains("org/lwjgl/opengl/GL20.glBlendEquationSeparate"));
        assertTrue(calls.contains(GL11_OWNER + ".glClearColor"));
        assertTrue(calls.contains(GL11_OWNER + ".glColorMask"));
        assertTrue(calls.contains(GL11_OWNER + ".glDepthMask"));
        assertTrue(calls.contains(GL11_OWNER + ".glAlphaFunc"));

        Set<String> framebufferCalls = callsInNested(
                "CallerGlState", "restoreFramebufferAndViewport");
        assertTrue(framebufferCalls.contains("org/lwjgl/opengl/GL30.glBindFramebuffer"));
        assertTrue(framebufferCalls.contains(GL11_OWNER + ".glViewport"));
    }

    @Test
    void compositeUsesControlledGuiStateAndExplicitBlendModels() throws Exception {
        Set<String> drawPass = callsIn("drawPass");
        assertTrue(drawPass.contains(GL11_OWNER + ".glDisable:" + GL11.GL_ALPHA_TEST));
        assertTrue(drawPass.contains(GL11_OWNER + ".glDisable:" + GL11.GL_DEPTH_TEST));
        assertTrue(drawPass.contains(GL11_OWNER + ".glDisable:" + GL11.GL_LIGHTING));
        assertTrue(drawPass.contains(GL11_OWNER + ".glDisable:" + GL11.GL_FOG));
        assertTrue(drawPass.contains(GL11_OWNER + ".glDisable:" + GL11.GL_CULL_FACE));
        assertTrue(drawPass.contains(GL11_OWNER + ".glDepthMask"));
        assertTrue(drawPass.contains(OWNER + ".setCompositeBlend"));
        assertTrue(drawPass.contains("org/lwjgl/opengl/GL20.glBlendEquationSeparate"));

        Set<String> blend = callsIn("setCompositeBlend");
        assertTrue(blend.contains("org/lwjgl/opengl/GL14.glBlendFuncSeparate"));
    }

    private static Set<String> callsIn(String method) throws Exception {
        return calls(BrilliantCaptureRenderer.class.getResourceAsStream(
                "/" + OWNER + ".class"), method);
    }

    private static Set<String> callsInNested(String nested, String method) throws Exception {
        return calls(BrilliantCaptureRenderer.class.getResourceAsStream(
                "/" + OWNER + "$" + nested + ".class"), method);
    }

    private static Set<String> calls(InputStream stream, String selectedMethod) throws Exception {
        assertNotNull(stream);
        Set<String> calls = new HashSet<>();
        try (InputStream input = stream) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!selectedMethod.equals(name)) return null;
                    return new IntTrackingVisitor(calls);
                }
            }, 0);
        }
        return calls;
    }

    /** Records qualified call names, plus the pushed capability for single-argument GL toggles. */
    static final class IntTrackingVisitor extends MethodVisitor {
        private final Set<String> calls;
        private Integer pushedInt;

        IntTrackingVisitor(Set<String> calls) {
            super(Opcodes.ASM9);
            this.calls = calls;
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
                pushedInt = opcode - Opcodes.ICONST_0;
            } else {
                pushedInt = null;
            }
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                pushedInt = operand;
            } else {
                pushedInt = null;
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            pushedInt = value instanceof Integer ? (Integer) value : null;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                    boolean isInterface) {
            String call = owner + "." + name;
            calls.add(call);
            if (pushedInt != null) calls.add(call + ":" + pushedInt);
            pushedInt = null;
        }
    }
}
