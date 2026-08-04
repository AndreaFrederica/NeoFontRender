package neofontrender.addons.chat;

/*
 * The completion state machine is adapted from Salutation 1.12.2 by Speiger
 * (Apache-2.0). It is kept in UIE's own controller so Salutation's ChatScreen
 * implementation is not part of the runtime dependency surface.
 */

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** UIE-owned command completion engine for the embedded Tabby chat input. */
public final class ChatCommandCompletionController {
    public static final ChatCommandCompletionController INSTANCE =
            new ChatCommandCompletionController();

    private final Map<GuiTextField, State> states = new WeakHashMap<>();

    private ChatCommandCompletionController() {}

    public static boolean handleKey(GuiTextField field, int keyCode) {
        return INSTANCE.handle(field, keyCode);
    }

    public static void afterKeyTyped(GuiTextField field, int keyCode) {
        INSTANCE.updateAfterKey(field, keyCode);
    }

    public static void setCompletions(GuiTextField field, String[] values) {
        INSTANCE.acceptCompletions(field, values);
    }

    private boolean handle(GuiTextField field, int keyCode) {
        State state = states.get(field);
        if (!enabled(field) || state == null || state.values.isEmpty()) return false;
        if (keyCode == Keyboard.KEY_TAB) {
            if (state.firstSelection) {
                state.firstSelection = false;
            } else if (GuiScreen.isShiftKeyDown()) {
                state.selected = state.selected == 0
                        ? state.values.size() - 1 : state.selected - 1;
            } else {
                state.selected = (state.selected + 1) % state.values.size();
            }
            state.select(state.selected);
            return true;
        }
        if (keyCode == Keyboard.KEY_UP) {
            state.move(-1);
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            state.move(1);
            return true;
        }
        return false;
    }

    private void updateAfterKey(GuiTextField field, int keyCode) {
        if (!enabled(field)) return;
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN
                || keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_PRIOR
                || keyCode == Keyboard.KEY_NEXT) {
            close(field);
            return;
        }
        request(field);
    }

    private void request(GuiTextField field) {
        if (field == null || !field.isFocused()) return;
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        String prefix = text.substring(0, cursor);
        if (prefix.isEmpty() || !prefix.startsWith("/")) {
            close(field);
            return;
        }
        State state = states.computeIfAbsent(field, State::new);
        state.beginRequest(wordStart(text, cursor));
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.player.connection == null) return;
        ClientCommandHandler.instance.autoComplete(prefix);
        minecraft.player.connection.sendPacket(new CPacketTabComplete(prefix,
                targetBlock(minecraft), false));
    }

    private static BlockPos targetBlock(Minecraft minecraft) {
        if (minecraft.objectMouseOver == null || minecraft.objectMouseOver.getBlockPos() == null) {
            return null;
        }
        return minecraft.objectMouseOver.getBlockPos();
    }

    private void acceptCompletions(GuiTextField field, String[] values) {
        if (!enabled(field)) return;
        State state = states.get(field);
        if (state == null || !state.acceptingResponses) return;
        List<String> next = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isEmpty()) next.add(value);
            }
        }
        String current = currentWord(field);
        next.removeIf(value -> value.equals(current));
        if (state.values.equals(next)) return;
        state.values.clear();
        state.values.addAll(next);
        if (state.values.isEmpty()) return;
        state.selected = 0;
        state.first = 0;
        state.firstSelection = true;
    }

    private String currentWord(GuiTextField field) {
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        return text.substring(wordStart(text, cursor), cursor);
    }

    static int wordStart(String text, int cursor) {
        int start = Math.max(0, Math.min(cursor, text == null ? 0 : text.length()));
        if (text == null) return start;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        return start;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void mouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiChat)) return;
        GuiTextField field = field((GuiChat) event.getGui());
        State state = states.get(field);
        if (!enabled(field) || state == null || state.values.isEmpty()) return;
        ChatSuggestionPopup.Layout layout = state.layout;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && layout != null && layout.rowAt(mouseX(), mouseY()) >= 0) {
            int delta = wheel > 0 ? -1 : 1;
            state.first = Math.max(0, Math.min(state.first + delta,
                    Math.max(0, state.values.size() - ChatSuggestionPopup.MAX_VISIBLE)));
            event.setCanceled(true);
            return;
        }
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        int row = layout == null ? -1 : layout.rowAt(mouseX(), mouseY());
        if (row < 0 || state.first + row >= state.values.size()) return;
        state.select(state.first + row);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiChat)) return;
        GuiTextField field = field((GuiChat) event.getGui());
        State state = states.get(field);
        if (!enabled(field) || state == null || state.values.isEmpty()) return;
        state.layout = ChatSuggestionPopup.draw(field, state.values, state.first,
                state.selected, ExternalChatCompat.getSalutationInput(field),
                event.getMouseX(), event.getMouseY(), Minecraft.getMinecraft().fontRenderer);
    }

    private static GuiTextField field(GuiChat chat) {
        return ((neofontrender.addons.mixin.AccessorGuiChatFeatures) (Object) chat)
                .nfrUi$getInputField();
    }

    private static boolean enabled(GuiTextField field) {
        return field != null && EnhancedChatConfigAccess.tabbedChatEnabled()
                && EnhancedChatConfigAccess.commandCompletionEnabled();
    }

    private void close(GuiTextField field) {
        State state = states.get(field);
        if (state != null) {
            state.values.clear();
            state.layout = null;
            state.acceptingResponses = false;
            state.firstSelection = false;
            state.selected = 0;
            state.first = 0;
            state.wordStart = -1;
        }
    }

    private static int mouseX() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return Mouse.getX() / resolution.getScaleFactor();
    }

    private static int mouseY() {
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        return resolution.getScaledHeight() - Mouse.getY() / resolution.getScaleFactor() - 1;
    }

    private static final class State {
        private final List<String> values = new ArrayList<>();
        private ChatSuggestionPopup.Layout layout;
        private int selected;
        private int first;
        private boolean firstSelection;
        private boolean acceptingResponses;
        private int wordStart = -1;
        private final GuiTextField owner;

        private State(GuiTextField owner) {
            this.owner = owner;
        }

        private void beginRequest(int nextWordStart) {
            // A delimiter starts a distinct candidate set. Reset immediately instead of leaving
            // the previous word selected until the network response arrives.
            if (wordStart != nextWordStart) {
                selected = 0;
                first = 0;
                firstSelection = true;
                wordStart = nextWordStart;
                values.clear();
                layout = null;
            }
            // For the same word (Tab cycles, edits inside it) keep showing the previous candidates
            // until the response arrives; if the solution is unchanged, acceptCompletions leaves
            // everything untouched so the popup never flickers.
            acceptingResponses = true;
        }

        private void move(int delta) {
            selected = (selected + delta + values.size()) % values.size();
            if (selected < first) first = selected;
            if (selected >= first + ChatSuggestionPopup.MAX_VISIBLE) {
                first = selected - ChatSuggestionPopup.MAX_VISIBLE + 1;
            }
            first = Math.max(0, Math.min(first,
                    Math.max(0, values.size() - ChatSuggestionPopup.MAX_VISIBLE)));
            select(selected);
        }

        private void select(int index) {
            if (index < 0 || index >= values.size()) return;
            GuiTextField field = owner;
            String text = field.getText();
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
            int start = wordStart(text, cursor);
            field.setCursorPosition(start);
            field.setSelectionPos(cursor);
            field.writeText(values.get(index));
            selected = index;
        }
    }
}
