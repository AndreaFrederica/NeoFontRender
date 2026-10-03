package neofontrender.core.font.postprocess;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowPostProcessorContractTest {
    @Test
    void prefersBackendNativeCompositionBeforeResolvingTheGenericShadowSource() throws Exception {
        InputStream stream = ShadowPostProcessor.class.getResourceAsStream(
                "/neofontrender/core/font/postprocess/ShadowPostProcessor.class");
        assertNotNull(stream);
        int[] index = {0};
        int[] nativeComposite = {-1};
        int[] genericSource = {-1};
        try (InputStream input = stream) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    if (!"process".equals(name)) return null;
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name,
                                                    String descriptor, boolean isInterface) {
                            int call = index[0]++;
                            if ("neofontrender/core/font/backend/TextRenderBackend".equals(owner)
                                    && "renderStructuredModernShadowAtSize".equals(name)) {
                                nativeComposite[0] = call;
                            }
                            if ("neofontrender/api/text/postprocess/TextPostProcessContext".equals(owner)
                                    && "shadowSource".equals(name)) {
                                genericSource[0] = call;
                            }
                        }
                    };
                }
            }, 0);
        }
        assertTrue(nativeComposite[0] >= 0, "native shadow capability must be queried");
        assertTrue(genericSource[0] > nativeComposite[0],
                "generic shadow source must remain a fallback");
    }
}
