package neofontrender.splash;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Adds AWT-backed draw/measure overrides to Forge's loading-screen font renderer.
 *
 * <p>The same target also supports ModernSplash, which replaces Forge's {@code SplashProgress$*}
 * classes with remapped copies of
 * {@code CustomSplash$*}. Its {@code fontRenderer} field is deliberately typed as its own
 * {@code SplashFontRenderer}, so replacing the field with an unrelated {@code FontRenderer}
 * subclass is not type-safe. Patching the renderer subclass itself keeps ModernSplash's field
 * and construction code intact.</p>
 */
public final class SplashProgressTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("Neo Font Render");
    private static final String TARGET =
            "net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer";
    private static final String SPLASH_COMPAT_INTERNAL =
            "neofontrender/splash/SplashCompat";
    private static final String WIDTH_METHOD = "func_78256_a";
    private static final String WIDTH_DESC = "(Ljava/lang/String;)I";
    private static final String DRAW_METHOD = "func_78276_b";
    private static final String DRAW_DESC = "(Ljava/lang/String;III)I";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (!TARGET.equals(name) && !TARGET.equals(transformedName)) {
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new SplashFontRendererVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            byte[] transformed = writer.toByteArray();
            LOGGER.info("Patched loading-screen font renderer bytecode");
            return transformed;
        } catch (Throwable t) {
            // Keep startup recoverable if ModernSplash changes its bytecode.
            LOGGER.error("Failed to patch loading-screen font renderer bytecode", t);
            return basicClass;
        }
    }

    private static final class SplashFontRendererVisitor extends ClassVisitor {
        private String superName;
        private boolean hasWidthOverride;
        private boolean hasDrawOverride;

        SplashFontRendererVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superName = superName;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (WIDTH_METHOD.equals(name) && WIDTH_DESC.equals(descriptor)) {
                hasWidthOverride = true;
            } else if (DRAW_METHOD.equals(name) && DRAW_DESC.equals(descriptor)) {
                hasDrawOverride = true;
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            if (!hasWidthOverride) {
                addWidthOverride();
            }
            if (!hasDrawOverride) {
                addDrawOverride();
            }
            super.visitEnd();
        }

        private void addWidthOverride() {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, WIDTH_METHOD, WIDTH_DESC, null, null);
            mv.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "isOverrideActive", "()Z", false);
            org.objectweb.asm.Label fallback = new org.objectweb.asm.Label();
            mv.visitJumpInsn(Opcodes.IFEQ, fallback);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "getStringWidth", WIDTH_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(fallback);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, WIDTH_METHOD, WIDTH_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private void addDrawOverride() {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, DRAW_METHOD, DRAW_DESC, null, null);
            mv.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "isOverrideActive", "()Z", false);
            org.objectweb.asm.Label fallback = new org.objectweb.asm.Label();
            mv.visitJumpInsn(Opcodes.IFEQ, fallback);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitVarInsn(Opcodes.ILOAD, 4);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "drawString", DRAW_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(fallback);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitVarInsn(Opcodes.ILOAD, 4);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, DRAW_METHOD, DRAW_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
}
