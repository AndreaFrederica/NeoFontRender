package neofontrender.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForgeDebugTextHookTest {
    @Test
    void registeredHooksCoverBothActualForgeColumnsWithoutWrappingHudEvents() throws Exception {
        try (var stream = getClass().getResourceAsStream("/mixins.neofontrender.json")) {
            assertNotNull(stream);
            assertTrue(new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .contains("\"MixinGuiIngameForgeDebugText\""));
        }
        ClassNode forge = readClass("net/minecraftforge/client/GuiIngameForge");
        var hud = forge.methods.stream().filter(m -> m.name.equals("renderHUDText")
                && m.desc.equals("(II)V")).findFirst().orElseThrow();
        List<String> calls = new ArrayList<>();
        for (var instruction : hud.instructions) {
            if (instruction instanceof MethodInsnNode call) {
                calls.add("L" + call.owner + ";" + call.name + call.desc);
            }
        }

        int widthMatches = 0;
        int drawMatches = 0;
        ClassNode mixin = readClass("neofontrender/mixin/MixinGuiIngameForgeDebugText");
        for (var method : mixin.methods) {
            if (method.visibleAnnotations == null) continue;
            for (var annotation : method.visibleAnnotations) {
                if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")) continue;
                assertEquals(List.of("renderHUDText"), value(annotation, "method"));
                AnnotationNode at = (AnnotationNode) value(annotation, "at");
                String target = (String) value(at, "target");
                assertTrue(target.startsWith("Lnet/minecraft/client/gui/FontRenderer;"),
                        "Event dispatch and other HUD calls must not be intercepted");
                int matches = (int) calls.stream().filter(target::equals).count();
                if (target.endsWith("(Ljava/lang/String;)I")) widthMatches += matches;
                else if (target.endsWith("(Ljava/lang/String;III)I")) drawMatches += matches;
                else fail("Unexpected debug font operation: " + target);
            }
        }
        assertEquals(2, widthMatches, "Both column widths must use the selected engine");
        assertEquals(2, drawMatches, "Both columns must draw with the same engine as measurement");
    }

    private static Object value(AnnotationNode annotation, String name) {
        int index = annotation.values.indexOf(name);
        assertTrue(index >= 0, name);
        return annotation.values.get(index + 1);
    }

    private static ClassNode readClass(String name) throws IOException {
        try (var stream = ForgeDebugTextHookTest.class.getResourceAsStream("/" + name + ".class")) {
            assertNotNull(stream, name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, 0);
            return node;
        }
    }
}
