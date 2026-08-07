package neofontrender.splash;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Patches ModernSplash/Forge loading screen classes:
 * <ol>
 *   <li>{@code SplashFontRenderer} — adds AWT-backed draw/measure overrides</li>
 *   <li>{@code SplashProgress$3} (rendering thread) — injects tip rendering
 *       before each {@code Display.update()} call</li>
 * </ol>
 */
public final class SplashProgressTransformer implements IClassTransformer {

    private static final Logger LOGGER = LogManager.getLogger("Revo Font");

    private static final String FONT_RENDERER_TARGET =
            "cpw.mods.fml.client.SplashProgress$SplashFontRenderer";
    private static final String SPLASH_COMPAT_INTERNAL =
            "neofontrender/splash/SplashCompat";
    private static final String DEOBFUSCATED_ENVIRONMENT = "fml.deobfuscatedEnvironment";
    private static final String WIDTH_METHOD_MCP = "getStringWidth";
    private static final String WIDTH_METHOD_SRG = "func_78256_a";
    private static final String WIDTH_DESC = "(Ljava/lang/String;)I";
    private static final String DRAW_METHOD_MCP = "drawString";
    private static final String DRAW_METHOD_SRG = "func_78276_b";
    private static final String DRAW_DESC = "(Ljava/lang/String;III)I";

    private static final String RENDER_THREAD_TARGET =
            "cpw.mods.fml.client.SplashProgress$3";
    private static final String TIPS_RENDERER_INTERNAL =
            "neofontrender/splash/SplashTipsRenderer";
    private static final String SPLASH_PROGRESS_INTERNAL =
            "cpw/mods/fml/client/SplashProgress";
    private static final String SPLASH_PROGRESS_CLASS =
            "cpw.mods.fml.client.SplashProgress";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        if (SPLASH_PROGRESS_CLASS.equals(name) || SPLASH_PROGRESS_CLASS.equals(transformedName)) {
            return patchSplashProgress(basicClass);
        }

        if (FONT_RENDERER_TARGET.equals(name) || FONT_RENDERER_TARGET.equals(transformedName)) {
            return patchFontRenderer(basicClass);
        }

        if (RENDER_THREAD_TARGET.equals(name) || RENDER_THREAD_TARGET.equals(transformedName)) {
            if (ModernSplashDetector.isInstalled()) {
                LOGGER.info("Found ModernSplash rendering thread: name={} transformedName={}", name, transformedName);
                return patchRenderThread(basicClass);
            }
            LOGGER.info("Found Forge vanilla splash rendering thread: name={} transformedName={}", name, transformedName);
            return patchForgeRenderThread(basicClass);
        }

        return basicClass;
    }

    private byte[] patchSplashProgress(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new SplashProgressVisitor(writer), ClassReader.EXPAND_FRAMES);
            LOGGER.info("Patched SplashProgress fields for tip rendering access");
            return writer.toByteArray();
        } catch (Throwable t) {
            LOGGER.error("Failed to patch SplashProgress fields", t);
            return basicClass;
        }
    }

    private static final class SplashProgressVisitor extends ClassVisitor {
        SplashProgressVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if ("fontRenderer".equals(name) || "fontColor".equals(name)) {
                access &= ~Opcodes.ACC_PRIVATE;
                access |= Opcodes.ACC_PUBLIC;
            }
            return super.visitField(access, name, descriptor, signature, value);
        }
    }

    private byte[] patchFontRenderer(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            boolean deobfuscated = Launch.blackboard != null
                    && Boolean.TRUE.equals(Launch.blackboard.get(DEOBFUSCATED_ENVIRONMENT));
            ClassVisitor visitor = new SplashFontRendererVisitor(writer,
                    deobfuscated ? WIDTH_METHOD_MCP : WIDTH_METHOD_SRG,
                    deobfuscated ? DRAW_METHOD_MCP : DRAW_METHOD_SRG);
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

    private byte[] patchForgeRenderThread(byte[] basicClass) {
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new ForgeRenderThreadVisitor(writer), ClassReader.EXPAND_FRAMES);
            LOGGER.info("Patched Forge vanilla rendering thread for tip display");
            return writer.toByteArray();
        } catch (Throwable t) {
            LOGGER.error("Failed to patch Forge vanilla rendering thread", t);
            return basicClass;
        }
    }

    private static final class SplashFontRendererVisitor extends ClassVisitor {
        private String superName;
        private final String widthMethod;
        private final String drawMethod;
        private boolean hasWidthOverride;
        private boolean hasDrawOverride;

        SplashFontRendererVisitor(ClassVisitor delegate, String widthMethod, String drawMethod) {
            super(Opcodes.ASM5, delegate);
            this.widthMethod = widthMethod;
            this.drawMethod = drawMethod;
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
            if (widthMethod.equals(name) && WIDTH_DESC.equals(descriptor)) {
                hasWidthOverride = true;
            } else if (drawMethod.equals(name) && DRAW_DESC.equals(descriptor)) {
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
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, widthMethod, WIDTH_DESC, null, null);
            mv.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "isOverrideActive", "()Z", false);
            Label fallback = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, fallback);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "getStringWidth", WIDTH_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(fallback);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, widthMethod, WIDTH_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private void addDrawOverride() {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, drawMethod, DRAW_DESC, null, null);
            mv.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, SPLASH_COMPAT_INTERNAL,
                    "isOverrideActive", "()Z", false);
            Label fallback = new Label();
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
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, drawMethod, DRAW_DESC, false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    /**
     * Patches SplashProgress$3.run() to inject tip rendering before Display.update().
     * Finds every INVOKESTATIC Display.update() and inserts a SplashTipsRenderer.render()
     * call before it.
     */
    private static final class RenderThreadVisitor extends ClassVisitor {
        private String className;

        RenderThreadVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM5, delegate);
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
                return new RunMethodVisitor(mv, className);
            }
            return mv;
        }
    }

    private static final class ForgeRenderThreadVisitor extends ClassVisitor {
        ForgeRenderThreadVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM5, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("run".equals(name) && "()V".equals(descriptor)) {
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String calledName,
                                                String calledDescriptor, boolean isInterface) {
                        if (opcode == Opcodes.INVOKESTATIC
                                && "org/lwjgl/opengl/Display".equals(owner)
                                && "update".equals(calledName)
                                && "()V".equals(calledDescriptor)) {
                            mv.visitFieldInsn(Opcodes.GETSTATIC, SPLASH_PROGRESS_INTERNAL,
                                    "fontRenderer",
                                    "Lcpw/mods/fml/client/SplashProgress$SplashFontRenderer;");
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display",
                                    "getWidth", "()I", false);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display",
                                    "getHeight", "()I", false);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, TIPS_RENDERER_INTERNAL,
                                    "renderForge", "(Ljava/lang/Object;II)V", false);
                        }
                        super.visitMethodInsn(opcode, owner, calledName, calledDescriptor, isInterface);
                    }
                };
            }
            return mv;
        }
    }

    /**
     * Intercepts the run() method to inject tip rendering before Display.update().
     */
    private static final class RunMethodVisitor extends MethodVisitor {
        private int methodInsnCount;
        private final String ownerClass;

        RunMethodVisitor(MethodVisitor delegate, String ownerClass) {
            super(Opcodes.ASM5, delegate);
            this.ownerClass = ownerClass;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            methodInsnCount++;
            if (opcode == Opcodes.INVOKESTATIC
                    && "org/lwjgl/opengl/Display".equals(owner)
                    && "update".equals(name)
                    && "()V".equals(descriptor)) {
                LOGGER.info("Injecting tip render before Display.update() at instruction #{}", methodInsnCount);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ownerClass,
                        "getString", "()Ljava/lang/String;", false);
                mv.visitFieldInsn(Opcodes.GETSTATIC, SPLASH_PROGRESS_INTERNAL,
                        "fontRenderer", "Lcpw/mods/fml/client/SplashProgress$SplashFontRenderer;");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display",
                        "getWidth", "()I", false);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display",
                        "getHeight", "()I", false);
                mv.visitFieldInsn(Opcodes.GETSTATIC, SPLASH_PROGRESS_INTERNAL,
                        "fontColor", "I");
                mv.visitVarInsn(Opcodes.FLOAD, 3);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TIPS_RENDERER_INTERNAL,
                        "render", "(Ljava/lang/String;Ljava/lang/Object;IIIF)V", false);
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
