package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.util.jar.JarFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Optional contract check against a user-supplied mod, without redistributing/loading its classes. */
class PregenBinaryContractTest {
    @Test void matchesInstalledPregenChatHooksAndAdapterSignatures() throws Exception {
        String path = System.getProperty("nfr.test.pregenJar", "");
        assumeTrue(!path.isEmpty(), "Supply -PpregenCompatJar to verify an installed mod JAR");
        try (JarFile jar = new JarFile(path)) {
            ClassNode handler = read(jar, "pregenerator/impl/client/ClientHandler");
            MethodNode open = method(handler, "onGuiOpen", "(Lnet/minecraftforge/client/event/GuiOpenEvent;)V");
            int chatReads = 0, boolReads = 0;
            for (AbstractInsnNode instruction : open.instructions) {
                if (instruction instanceof FieldInsnNode field && field.name.equals("disableAdvChat")) chatReads++;
                if (instruction instanceof MethodInsnNode call && call.owner.equals("carbonconfiglib/config/ConfigEntry$BoolValue")
                        && call.name.equals("get") && call.desc.equals("()Z")) boolReads++;
            }
            assertEquals(2, chatReads, "Both HUD and input replacement must be covered");
            assertTrue(boolReads > chatReads, "The handler also reads unrelated options that must be preserved");
            ClassNode completer = read(jar, "pregenerator/impl/client/gui/chat/ChatScreen$Completor");
            method(completer, "<init>", "(Lnet/minecraft/client/gui/GuiTextField;)V");
            ClassNode base = read(jar, completer.superName);
            method(base, "requestUpdate", "()V");
            method(base, "render", "(IILnet/minecraft/client/gui/FontRenderer;)V");
            method(base, "select", "(I)V");
            method(base, "func_186840_a", "([Ljava/lang/String;)V");
            assertTrue(base.fields.stream().anyMatch(f -> f.name.equals("offset") && f.desc.equals("I")));
            ClassNode box = read(jar, "pregenerator/impl/client/gui/chat/AdvancedTabCompleter$ClickBox");
            for (String name : new String[] {"getX", "getY", "getWidth", "getHeight"}) method(box, name, "()I");
        }
    }

    private static ClassNode read(JarFile jar, String name) throws Exception {
        ClassNode node = new ClassNode();
        try (java.io.InputStream input = jar.getInputStream(jar.getJarEntry(name + ".class"))) {
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private static MethodNode method(ClassNode type, String name, String descriptor) {
        return type.methods.stream().filter(m -> m.name.equals(name) && m.desc.equals(descriptor))
                .findFirst().orElseThrow(() -> new AssertionError(type.name + "." + name + descriptor));
    }
}
