package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Live @player suggestions shared by vanilla and embedded Tabby chat. */
public final class MentionCompletionController {
    public static final MentionCompletionController INSTANCE = new MentionCompletionController();

    private final List<String> matches = new ArrayList<>();
    private GuiTextField input;
    private ChatSuggestionPopup.Layout layout;
    private int tokenStart = -1;
    private int tokenEnd = -1;
    private int selected;
    private int firstVisible;

    private MentionCompletionController() {}

    static boolean handleKey(GuiTextField field, int keyCode) {
        INSTANCE.refresh(field);
        if (!INSTANCE.isOpen()) return false;
        if (keyCode == Keyboard.KEY_UP) {
            INSTANCE.move(-1);
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            INSTANCE.move(1);
            return true;
        }
        if (keyCode == Keyboard.KEY_TAB || keyCode == Keyboard.KEY_RETURN
                || keyCode == Keyboard.KEY_NUMPADENTER) {
            INSTANCE.accept(field, INSTANCE.selected);
            return true;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            INSTANCE.close();
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void opened(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiChat)) close();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiChat) || !isOpen()
                || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        int row = layout == null ? -1 : layout.rowAt(mouseX(), mouseY());
        if (row < 0 || firstVisible + row >= matches.size()) return;
        accept(input, firstVisible + row);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiChat)) {
            close();
            return;
        }
        GuiTextField field = ((AccessorGuiChatFeatures) event.getGui()).nfrUi$getInputField();
        refresh(field);
        if (!isOpen()) return;
        layout = ChatSuggestionPopup.draw(field, matches, firstVisible, selected,
                ExternalChatCompat.getSalutationInput(field), event.getMouseX(), event.getMouseY(),
                Minecraft.getMinecraft().fontRenderer, true);
    }

    private void refresh(GuiTextField field) {
        input = field;
        if (!EnhancedChatConfig.enabled || !EnhancedChatConfig.mentionCompletion
                || field == null || !field.isFocused()
                || Minecraft.getMinecraft().getConnection() == null) {
            closeCandidates();
            return;
        }
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        String token = text.substring(start, cursor);
        if (!token.startsWith("@") || token.indexOf('@', 1) >= 0) {
            closeCandidates();
            return;
        }
        String prefix = token.substring(1).toLowerCase(Locale.ROOT);
        String selectedName = selected >= 0 && selected < matches.size() ? matches.get(selected) : "";
        List<String> next = new ArrayList<>();
        for (NetworkPlayerInfo info : Minecraft.getMinecraft().getConnection().getPlayerInfoMap()) {
            String name = info.getGameProfile().getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) next.add("@" + name);
        }
        next.sort(String.CASE_INSENSITIVE_ORDER);
        // Reuse the existing candidate list when the solution is unchanged so the popup
        // does not flicker through a clear-and-rebuild every frame.
        if (!next.equals(matches)) {
            matches.clear();
            matches.addAll(next);
            selected = Math.max(0, matches.indexOf(selectedName));
        }
        tokenStart = start;
        tokenEnd = cursor;
        keepSelectedVisible();
        if (matches.isEmpty()) closeCandidates();
    }

    private void move(int distance) {
        selected = (selected + distance + matches.size()) % matches.size();
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
        String replacement = matches.get(index);
        if (end >= text.length() || !Character.isWhitespace(text.charAt(end))) replacement += " ";
        field.writeText(replacement);
        closeCandidates();
    }

    private boolean isOpen() {
        return input != null && !matches.isEmpty() && tokenStart >= 0;
    }

    static void reset() { INSTANCE.closeCandidates(); }

    private void close() {
        input = null;
        closeCandidates();
    }

    private void closeCandidates() {
        matches.clear();
        layout = null;
        tokenStart = -1;
        tokenEnd = -1;
        selected = 0;
        firstVisible = 0;
    }

    private static int mouseX() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int mouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }
}
