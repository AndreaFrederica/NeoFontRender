package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.inline.InlineGlyphMiddleware;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/** Gosling-style visual alias picker integrated into UIE's existing chat completion flow. */
public final class EmojiCompletionController {
    public static final EmojiCompletionController INSTANCE = new EmojiCompletionController();

    private final List<String> matches = new ArrayList<>();
    private GuiTextField input;
    private ChatSuggestionPopup.Layout layout;
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private int selected;
    private int firstVisible;

    private EmojiCompletionController() {}

    static boolean handleKey(GuiTextField field, int keyCode) {
        INSTANCE.refresh(field);
        if (!INSTANCE.isOpen()) return false;
        if (keyCode == Keyboard.KEY_UP) { INSTANCE.move(-1); return true; }
        if (keyCode == Keyboard.KEY_DOWN) { INSTANCE.move(1); return true; }
        if (keyCode == Keyboard.KEY_TAB || keyCode == Keyboard.KEY_RETURN
                || keyCode == Keyboard.KEY_NUMPADENTER) {
            INSTANCE.accept(field, INSTANCE.selected);
            return true;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) { INSTANCE.closeCandidates(); return true; }
        return false;
    }

    @SubscribeEvent
    public void opened(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiChat)) close();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(MouseEvent event) {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChat) || !isOpen()
                || event.button != 0 || !event.buttonstate) return;
        int row = layout == null ? -1 : layout.rowAt(mouseX(), mouseY());
        if (row < 0 || firstVisible + row >= matches.size()) return;
        accept(input, firstVisible + row);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) { close(); return; }
        GuiTextField field = ((AccessorGuiChatFeatures) event.gui).nfrUi$getInputField();
        refresh(field);
        if (!isOpen()) return;
        layout = ChatSuggestionPopup.draw(field, matches, firstVisible, selected,
                ExternalChatCompat.getSalutationInput(field), event.mouseX, event.mouseY,
                Minecraft.getMinecraft().fontRenderer);
    }

    private void refresh(GuiTextField field) {
        input = field;
        if ((!EnhancedChatFeatures.goslingImageGlyphs() && !EnhancedChatFeatures.localImageGlyphs())
                || field == null || !field.isFocused()) {
            closeCandidates();
            return;
        }
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        String token = text.substring(start, cursor);
        if (!token.startsWith(":") || token.indexOf(':', 1) >= 0) {
            closeCandidates();
            return;
        }
        String prefix = token.substring(1);
        if (!prefix.matches("[\\w+\\-]*")) { closeCandidates(); return; }
        List<String> next = InlineGlyphMiddleware.emojiSuggestions(prefix, 200);
        if (!next.equals(matches)) {
            String old = selected >= 0 && selected < matches.size() ? matches.get(selected) : "";
            matches.clear();
            matches.addAll(next);
            selected = Math.max(0, matches.indexOf(old));
        }
        tokenStart = start;
        tokenEnd = cursor;
        keepSelectedVisible();
        if (matches.isEmpty()) closeCandidates();
    }

    private void move(int delta) {
        selected = (selected + delta + matches.size()) % matches.size();
        keepSelectedVisible();
    }

    private void keepSelectedVisible() {
        if (selected < firstVisible) firstVisible = selected;
        if (selected >= firstVisible + ChatSuggestionPopup.MAX_VISIBLE) {
            firstVisible = selected - ChatSuggestionPopup.MAX_VISIBLE + 1;
        }
        firstVisible = Math.max(0, Math.min(firstVisible,
                Math.max(0, matches.size() - ChatSuggestionPopup.MAX_VISIBLE)));
    }

    private void accept(GuiTextField field, int index) {
        if (field == null || index < 0 || index >= matches.size()) return;
        String text = field.getText();
        int end = Math.max(tokenStart, Math.min(tokenEnd, text.length()));
        field.setCursorPosition(tokenStart);
        field.setSelectionPos(end);
        field.writeText(matches.get(index));
        closeCandidates();
    }

    private boolean isOpen() { return input != null && !matches.isEmpty() && tokenStart >= 0; }

    private void close() { input = null; closeCandidates(); }

    private void closeCandidates() {
        matches.clear();
        layout = null;
        tokenStart = tokenEnd = -1;
        selected = firstVisible = 0;
    }

    private static int mouseX() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int mouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }
}
