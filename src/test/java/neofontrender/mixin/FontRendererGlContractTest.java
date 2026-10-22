package neofontrender.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontRendererGlContractTest {
    @Test
    void enablesAlphaBeforeAnyCancellableDrawStringPath() throws Exception {
        InputStream stream = MixinFontRenderer.class.getResourceAsStream(
                "/neofontrender/mixin/MixinFontRenderer.class");
        assertNotNull(stream);

        int[] methodCallIndex = {0};
        int[] enableAlphaIndex = {-1};
        int[] driverEnableAlphaIndex = {-1};
        int[] firstCancellablePathIndex = {-1};
        try (InputStream input = stream) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!"sfr$onDrawString".equals(name)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String calledName,
                                                    String calledDescriptor, boolean isInterface) {
                            int index = methodCallIndex[0]++;
                            if ("net/minecraft/client/renderer/GlStateManager".equals(owner)
                                    && "enableAlpha".equals(calledName)) {
                                enableAlphaIndex[0] = index;
                            }
                            if ("org/lwjgl/opengl/GL11".equals(owner)
                                    && "glEnable".equals(calledName)) {
                                driverEnableAlphaIndex[0] = index;
                            }
                            if ("neofontrender/api/text/route/TextRenderRouteApi".equals(owner)
                                    && "layout".equals(calledName)) {
                                firstCancellablePathIndex[0] = index;
                            }
                        }
                    };
                }
            }, 0);
        }

        assertTrue(enableAlphaIndex[0] >= 0, "drawString hook must enable alpha");
        assertTrue(driverEnableAlphaIndex[0] >= 0,
                "drawString hook must synchronize the OpenGL driver state");
        assertTrue(firstCancellablePathIndex[0] >= 0,
                "expected the unified render-route path");
        assertTrue(enableAlphaIndex[0] < firstCancellablePathIndex[0],
                "alpha must be enabled before a cancellable rendering path can return");
        assertTrue(driverEnableAlphaIndex[0] < firstCancellablePathIndex[0],
                "driver alpha state must be synchronized before a rendering path can return");
    }
}
