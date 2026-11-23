package neofontrender.core.font.postprocess;

import org.junit.jupiter.api.Test;
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

class BrilliantCaptureRendererGlContractTest {
    private static final String OWNER =
            "neofontrender/core/font/postprocess/BrilliantCaptureRenderer";

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
        assertFalse(particleCalls.contains(
                "net/minecraft/client/renderer/GlStateManager.enableDepth"));
        assertFalse(particleCalls.contains(
                "net/minecraft/client/renderer/GlStateManager.enableLighting"));
    }

    @Test
    void callerStateRepairsDriverAndMinecraftCaches() throws Exception {
        Set<String> calls = callsInNested("CallerGlState", "close");
        assertTrue(calls.contains("org/lwjgl/opengl/GL20.glUseProgram"));
        assertTrue(calls.contains(OWNER + "$CallerGlState.restoreFramebufferAndViewport"));
        assertTrue(calls.contains(
                "net/minecraft/client/renderer/GlStateManager.tryBlendFuncSeparate"));
        assertTrue(calls.contains(
                "net/minecraft/client/renderer/GlStateManager.clearColor"));
        assertTrue(calls.contains(OWNER + "$CallerGlState.restoreTextureUnits"));

        Set<String> framebufferCalls = callsInNested(
                "CallerGlState", "restoreFramebufferAndViewport");
        assertTrue(framebufferCalls.contains("org/lwjgl/opengl/GL30.glBindFramebuffer"));
        assertTrue(framebufferCalls.contains(
                "net/minecraft/client/renderer/GlStateManager.viewport"));
    }

    @Test
    void compositeUsesControlledGuiStateAndExplicitBlendModels() throws Exception {
        Set<String> drawPass = callsIn("drawPass");
        assertTrue(drawPass.contains("net/minecraft/client/renderer/GlStateManager.disableAlpha"));
        assertTrue(drawPass.contains("net/minecraft/client/renderer/GlStateManager.disableDepth"));
        assertTrue(drawPass.contains("net/minecraft/client/renderer/GlStateManager.disableLighting"));
        assertTrue(drawPass.contains("net/minecraft/client/renderer/GlStateManager.disableFog"));
        assertTrue(drawPass.contains("net/minecraft/client/renderer/GlStateManager.disableCull"));
        assertTrue(drawPass.contains(OWNER + ".setCompositeBlend"));

        Set<String> blend = callsIn("setCompositeBlend");
        assertTrue(blend.contains(
                "net/minecraft/client/renderer/GlStateManager.tryBlendFuncSeparate"));
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
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                                    String descriptor, boolean isInterface) {
                            calls.add(owner + "." + name);
                        }
                    };
                }
            }, 0);
        }
        return calls;
    }
}
