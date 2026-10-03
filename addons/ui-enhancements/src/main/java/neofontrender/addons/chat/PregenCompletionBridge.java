package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Uses the installed mod's completer, never its ChatScreen. No optional classes in signatures. */
final class PregenCompletionBridge {
    private static final String COMPLETER = "pregenerator.impl.client.gui.chat.ChatScreen$Completor";
    private static Api api;
    private static boolean checked;
    private static boolean failed;

    private final GuiTextField mirror;
    private final TabCompleter completer;

    static boolean available() {
        if (failed) return false;
        if (!checked) {
            checked = true;
            // Called after mod discovery, from the settings/runtime chat path only.
            if (PregenCompletionBridge.class.getClassLoader().getResource(COMPLETER.replace('.', '/') + ".class") == null)
                return false;
            try { api = new Api(); }
            catch (ReflectiveOperationException | LinkageError | RuntimeException exception) { fail(exception); }
        }
        return api != null && !failed;
    }

    static PregenCompletionBridge create() {
        if (!available()) return null;
        try { return new PregenCompletionBridge(); }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            fail(exception);
            return null;
        }
    }

    private PregenCompletionBridge() throws ReflectiveOperationException {
        mirror = new GuiTextField(0, Minecraft.getMinecraft().fontRenderer, 0, 0, 1, 12);
        mirror.setMaxStringLength(32767);
        completer = (TabCompleter) api.constructor.newInstance(mirror);
    }

    private void sync(GuiTextField input) {
        mirror.setMaxStringLength(input.getMaxStringLength());
        mirror.setText(input.getText());
        mirror.setCursorPosition(input.getCursorPosition());
        mirror.setSelectionPos(input.getSelectionEnd());
        mirror.setFocused(true);
    }

    void request(GuiTextField input) {
        sync(input);
        try { api.request.invoke(completer); }
        catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            // The call may already have sent a packet. Never send a second request as a retry.
            fail(exception);
        }
    }

    List<String> candidates(GuiTextField input, CommandCompletionCandidates.Merge merged) {
        sync(input);
        try {
            completer.setCompletions(merged.plainValues());
            List<String> result = new ArrayList<>();
            for (String value : values()) result.add(CommandCompletionCandidates.styled(value, merged.sourceOf(value)));
            return result;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            fail(exception);
            return merged.styledValues();
        }
    }

    boolean commit(GuiTextField input, List<String> candidates, int index) {
        sync(input);
        try {
            snapshot(candidates, 0, index);
            api.select.invoke(completer, index);
            input.setText(mirror.getText());
            input.setCursorPosition(mirror.getCursorPosition());
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            fail(exception);
            return false;
        }
    }

    ChatSuggestionPopup.Layout draw(GuiTextField input, List<String> candidates, int first, int selected,
                                    ExternalChatCompat.InputGeometry geometry, int mouseX, int mouseY, FontRenderer font) {
        sync(input);
        mirror.x = geometry == null ? input.x : geometry.x;
        mirror.y = geometry == null ? input.y : geometry.y;
        mirror.width = geometry == null ? input.width : geometry.width;
        try {
            snapshot(candidates, first, selected);
            api.render.invoke(completer, mouseX, mouseY, font);
            Object box = api.box.get(completer);
            int x = (Integer) api.boxX.invoke(box);
            int y = (Integer) api.boxY.invoke(box);
            int width = (Integer) api.boxWidth.invoke(box);
            int height = (Integer) api.boxHeight.invoke(box);
            return ChatSuggestionPopup.uniformLayout(x, y, width, height,
                    Math.min(10, candidates.size() - first), 12);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            fail(exception);
            return ChatSuggestionPopup.draw(input, candidates, first, selected, geometry, mouseX, mouseY, font);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> values() throws IllegalAccessException {
        return (List<String>) api.values.get(completer);
    }

    private void snapshot(List<String> candidates, int first, int selected) throws IllegalAccessException {
        List<String> values = values();
        values.clear();
        for (String value : candidates) values.add(CommandCompletionCandidates.plain(value));
        api.offset.setInt(completer, first);
        api.selected.setInt(completer, selected);
    }

    private static void fail(Throwable exception) {
        if (!failed) neofontrender.NeoFontRender.LOGGER.warn(
                "Chunk Pregenerator completion adapter unavailable; using UIE", exception);
        failed = true;
    }

    private static final class Api {
        final Constructor<?> constructor;
        final Method request, render, select, boxX, boxY, boxWidth, boxHeight;
        final Field values, selected, offset, box;

        Api() throws ReflectiveOperationException {
            ClassLoader loader = PregenCompletionBridge.class.getClassLoader();
            Class<?> type = Class.forName(COMPLETER, false, loader);
            Class<?> base = type.getSuperclass();
            constructor = type.getConstructor(GuiTextField.class);
            request = type.getMethod("requestUpdate");
            render = type.getMethod("render", int.class, int.class, FontRenderer.class);
            select = type.getMethod("select", int.class);
            values = ReflectionHelper.findField(TabCompleter.class, "completions", "field_186849_f");
            selected = ReflectionHelper.findField(TabCompleter.class, "completionIdx", "field_186848_e");
            offset = base.getDeclaredField("offset");
            offset.setAccessible(true);
            box = base.getDeclaredField("box");
            box.setAccessible(true);
            Class<?> boxType = box.getType();
            boxX = boxType.getMethod("getX");
            boxY = boxType.getMethod("getY");
            boxWidth = boxType.getMethod("getWidth");
            boxHeight = boxType.getMethod("getHeight");
        }
    }
}
