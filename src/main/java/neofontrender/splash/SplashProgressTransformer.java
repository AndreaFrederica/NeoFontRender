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
 * Patches ModernSplash/Forge loading screen classes:
 * <ol>
 *   <li>{@code SplashFontRenderer} — adds AWT-backed draw/measure overrides</li>
 *   <li>{@code SplashProgress$2} (rendering thread) — injects tip rendering
 *       before each {@code Display.update()} call</li>
 * </ol>
 */
public final class SplashProgressTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("Neo Font Render");

    // SplashFontRenderer targets
    private static final String FONT_RENDERER_TARGET =
            "net.minecraftforge.fml.client.SplashProgress$SplashFontRenderer";
    private static final String SPLASH_COMPAT_INTERNAL =
            "neofontrender/splash/SplashCompat";
    private static final String WIDTH_METHOD = "func_78256_a";
    private static final String WIDTH_DESC = "(Ljava/lang/String;)I";
    private static final String DRAW_METHOD = "func_78276_b";
    private static final String DRAW_DESC = "(Ljava/lang/String;III)I";

    // Rendering thread targets
    private static final String RENDER_THREAD_TARGET =
            "net.minecraftforge.fml.client.SplashProgress$2";
    private static final String TIPS_RENDERER_INTERNAL =
            "neofontrender/splash/SplashTipsRenderer";
    private static final String SPLASH_PROGRESS_INTERNAL =
            "net/minecraftforge/fml/client/SplashProgress";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        // Patch SplashFontRenderer
        if (FONT_RENDERER_TARGET.equals(name) || FONT_RENDERER_TARGET.equals(transformedName)) {
            return patchFontRenderer(basicClass);
        }

        // Patch rendering thread (SplashProgress$2)
        if (RENDER_THREAD_TARGET.equals(name) || RENDER_THREAD_TARGET.equals(transformedName)) {
            return patchRenderThread(basicClass);
        }

        return basicClass;
    }

    private byte[] patchFontRenderer(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new SplashFontRendererVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            byte[] transformed = writer.toByteArray();
            LOGGER.info("Patched loading-screen font renderer bytecode");
            return transformed;
        } catch (Throwable t) {
            LOGGER.error("Failed to patch loading-screen font renderer bytecode", t);
            return basicClass;
        }
    }

    private byte[] patchRenderThread(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new RenderThreadVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            byte[] transformed = writer.toByteArray();
            LOGGER.info("Patched ModernSplash rendering thread for tip display");
            return transformed;
        } catch (Throwable t) {
            LOGGER.error("Failed to patch ModernSplash rendering thread", t);
            return basicClass;
        }
    }

    /**
     * Patches SplashFontRenderer to add AWT-backed draw/measure overrides.
     */
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

    /**
     * Patches SplashProgress$2.run() to inject tip rendering before Display.update().
     * Finds every INVOKESTATIC Display.update() and inserts a SplashTipsRenderer.render()
     * call before it.
     */
    private static final class RenderThreadVisitor extends ClassVisitor {
        private String className;

        RenderThreadVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("run".equals(name) && "()V".equals(descriptor)) {
                return new RunMethodVisitor(mv);
            }
            return mv;
        }
    }

    /**
     * Intercepts the run() method to inject tip rendering before Display.update().
     */
    private static final class RunMethodVisitor extends MethodVisitor {
        private boolean injected;

        RunMethodVisitor(MethodVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            // Inject tip rendering before Display.update() calls
            if (!injected
                    && opcode == Opcodes.INVOKESTATIC
                    && "org/lwjgl/opengl/Display".equals(owner)
                    && "update".equals(name)
                    && "()V".equals(descriptor)) {
                // Push SplashProgress.fontRenderer (static field)
                mv.visitFieldInsn(Opcodes.GETSTATIC, SPLASH_PROGRESS_INTERNAL,
                        "fontRenderer", "Lnet/minecraftforge/fml/client/SplashProgress$SplashFontRenderer;");
                // Push screen dimensions (ModernSplash uses 640x480 ortho)
                mv.visitIntInsn(Opcodes.SIPUSH, 640);
                mv.visitIntInsn(Opcodes.SIPUSH, 480);
                // Call SplashTipsRenderer.render(Object, int, int)
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TIPS_RENDERER_INTERNAL,
                        "render", "(Ljava/lang/Object;II)V", false);
                injected = true;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
