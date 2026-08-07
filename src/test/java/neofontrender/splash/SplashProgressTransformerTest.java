package neofontrender.splash;

import net.minecraft.launchwrapper.Launch;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplashProgressTransformerTest {

    private static final String DEOBFUSCATED_ENVIRONMENT = "fml.deobfuscatedEnvironment";
    private static final String TARGET =
            "cpw.mods.fml.client.SplashProgress$SplashFontRenderer";
    private static final String RENDER_THREAD_TARGET =
            "cpw.mods.fml.client.SplashProgress$3";

    @Test
    void addsForge1710SrgDrawAndWidthOverrides() {
        assertOverrides(false, "func_78256_a", "func_78276_b");
    }

    @Test
    void addsForge1710McpDrawAndWidthOverridesInDevelopment() {
        assertOverrides(true, "getStringWidth", "drawString");
    }

    private static void assertOverrides(boolean deobfuscated, String widthMethod, String drawMethod) {
        ClassWriter fixture = new ClassWriter(0);
        fixture.visit(Opcodes.V1_8, Opcodes.ACC_FINAL, TARGET.replace('.', '/'), null,
                "net/minecraft/client/gui/FontRenderer", null);
        fixture.visitEnd();

        boolean blackboardWasNull = Launch.blackboard == null;
        if (blackboardWasNull) {
            Launch.blackboard = new HashMap<>();
        }
        Object previous = Launch.blackboard.put(DEOBFUSCATED_ENVIRONMENT, deobfuscated);
        byte[] transformed;
        try {
            transformed = new SplashProgressTransformer().transform(
                    TARGET, TARGET, fixture.toByteArray());
        } finally {
            if (blackboardWasNull) {
                Launch.blackboard = null;
            } else if (previous == null) {
                Launch.blackboard.remove(DEOBFUSCATED_ENVIRONMENT);
            } else {
                Launch.blackboard.put(DEOBFUSCATED_ENVIRONMENT, previous);
            }
        }

        Set<String> methods = new HashSet<>();
        Set<String> bridgeCalls = new HashSet<>();
        new ClassReader(transformed).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                methods.add(name + descriptor);
                return new MethodVisitor(Opcodes.ASM5) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String calledName,
                                                String calledDescriptor, boolean isInterface) {
                        if ("neofontrender/splash/SplashCompat".equals(owner)) {
                            bridgeCalls.add(calledName + calledDescriptor);
                        }
                    }
                };
            }
        }, 0);

        assertTrue(methods.contains(widthMethod + "(Ljava/lang/String;)I"));
        assertTrue(methods.contains(drawMethod + "(Ljava/lang/String;III)I"));
        assertTrue(bridgeCalls.contains("isOverrideActive()Z"));
        assertTrue(bridgeCalls.contains("getStringWidth(Ljava/lang/String;)I"));
        assertTrue(bridgeCalls.contains("drawString(Ljava/lang/String;III)I"));
    }

    @Test
    void leavesUnrelatedClassesUntouched() {
        byte[] fixture = new byte[] {1, 2, 3};
        byte[] transformed = new SplashProgressTransformer().transform(
                "example.Unrelated", "example.Unrelated", fixture);
        assertEquals(fixture, transformed);
    }

    @Test
    void injectsTipsBeforeEveryDisplayUpdate() {
        ClassWriter fixture = new ClassWriter(0);
        fixture.visit(Opcodes.V1_8, Opcodes.ACC_FINAL, RENDER_THREAD_TARGET, null,
                "java/lang/Object", null);
        MethodVisitor run = fixture.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        run.visitCode();
        run.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display",
                "update", "()V", false);
        run.visitMethodInsn(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/Display",
                "update", "()V", false);
        run.visitInsn(Opcodes.RETURN);
        run.visitMaxs(0, 0);
        run.visitEnd();
        fixture.visitEnd();

        byte[] transformed = new SplashProgressTransformer().transform(
                RENDER_THREAD_TARGET, RENDER_THREAD_TARGET, fixture.toByteArray());

        final int[] calls = {0};
        new ClassReader(transformed).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor delegate = super.visitMethod(
                        access, name, descriptor, signature, exceptions);
                if (!"run".equals(name)) return delegate;
                return new MethodVisitor(Opcodes.ASM5, delegate) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String calledName,
                                                String calledDescriptor, boolean isInterface) {
                        if ("neofontrender/splash/SplashTipsRenderer".equals(owner)
                                && ("render".equals(calledName)
                                        || "renderForge".equals(calledName))) {
                            calls[0]++;
                        }
                        super.visitMethodInsn(
                                opcode, owner, calledName, calledDescriptor, isInterface);
                    }
                };
            }
        }, 0);

        assertEquals(2, calls[0]);
    }
}
