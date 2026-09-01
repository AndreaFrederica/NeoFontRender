package neofontrender.addons.chat;

/*
 * The completion state machine is adapted from Salutation 1.12.2 by Speiger
 * (Apache-2.0). It is kept in UIE's own controller so Salutation's ChatScreen
 * implementation is not part of the runtime dependency surface.
 */

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.ClientCommandHandler;
import neofontrender.addons.api.command.CommandCompletionPosition;
import neofontrender.addons.api.command.client.ClientCommandCompletionApi;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
        if (keyCode == Keyboard.KEY_ESCAPE) {
            state.dismiss();
            return true;
        }
        if (keyCode == Keyboard.KEY_TAB) {
            state.commit(state.selected < 0 ? 0 : state.selected);
            request(field);
            return true;
        }
        if ((keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
                && state.selected >= 0) {
            state.commit(state.selected);
            request(field);
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
        if (!state.shouldRequest(prefix)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.player.connection == null) return;
        BlockPos target = targetBlock(minecraft);
        ClientCommandHandler.instance.autoComplete(prefix);
        String[] clientValues = ClientCommandCompletionApi.resolve(prefix,
                completionPosition(target), ClientCommandHandler.instance.latestAutoComplete);
        state.beginRequest(prefix, tokenRange(text, cursor), clientValues);
        minecraft.player.connection.sendPacket(new CPacketTabComplete(prefix, target, false));
    }

    private static BlockPos targetBlock(Minecraft minecraft) {
        if (minecraft.objectMouseOver == null || minecraft.objectMouseOver.getBlockPos() == null) {
            return null;
        }
        return minecraft.objectMouseOver.getBlockPos();
    }

    private static CommandCompletionPosition completionPosition(BlockPos pos) {
        return pos == null ? null : new CommandCompletionPosition(
                pos.getX(), pos.getY(), pos.getZ());
    }

    private void acceptCompletions(GuiTextField field, String[] values) {
        State state = states.get(field);
        if (state == null) return;
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        String currentPrefix = text.substring(0, cursor);
        Request accepted = state.acceptResponse(currentPrefix);
        if (accepted == null || !enabled(field)) return;
        List<String> next = CommandCompletionCandidates.merge(
                values, accepted.clientValues).styledValues();
        CommandCompletionPresentation.rememberRootCandidates(currentPrefix, next);
        TokenRange range = tokenRange(text, cursor);
        String current = text.substring(range.start, range.end);
        boolean exactMatch = next.stream().anyMatch(value -> sameCandidate(value, current));
        if (exactMatch && (cursor < range.end || next.size() == 1)) {
            state.dismiss();
            return;
        }
        next.removeIf(value -> sameCandidate(value, current));
        if (state.values.equals(next)) return;
        state.values.clear();
        state.values.addAll(next);
        if (state.values.isEmpty()) {
            state.dismiss();
            return;
        }
        state.selected = -1;
        state.first = 0;
        CommandCompletionPresentation.update(field, state.values, state.selected);
    }

    private static boolean sameCandidate(String left, String right) {
        String first = normalizedCandidate(left);
        String second = normalizedCandidate(right);
        return first.equalsIgnoreCase(second);
    }

    private static String normalizedCandidate(String value) {
        String plain = CommandCompletionCandidates.plain(value);
        return plain.startsWith("/") ? plain.substring(1) : plain;
    }

    static int wordStart(String text, int cursor) {
        int start = Math.max(0, Math.min(cursor, text == null ? 0 : text.length()));
        if (text == null) return start;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) start--;
        return start;
    }

    static int wordEnd(String text, int cursor) {
        if (text == null) return 0;
        int end = Math.max(0, Math.min(cursor, text.length()));
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) end++;
        return end;
    }

    static TokenRange tokenRange(String text, int cursor) {
        return new TokenRange(wordStart(text, cursor), wordEnd(text, cursor));
    }

    static String insertionValue(String text, TokenRange range, String candidate) {
        String plain = CommandCompletionCandidates.plain(candidate);
        if (range.start == 0 && text != null && text.startsWith("/")
                && !plain.startsWith("/")) {
            return "/" + plain;
        }
        return plain;
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
        state.commit(state.first + row);
        request(field);
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
        if (state != null) state.dismiss();
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
        private int wordStart = -1;
        private final RequestTracker requests = new RequestTracker();
        private final WeakReference<GuiTextField> owner;

        private State(GuiTextField owner) {
            this.owner = new WeakReference<>(owner);
        }

        private boolean shouldRequest(String prefix) {
            return requests.shouldRequest(prefix);
        }

        private void beginRequest(String prefix, TokenRange range, String[] nextClientValues) {
            // A delimiter starts a distinct candidate set. Reset immediately instead of leaving
            // the previous word selected until the network response arrives.
            if (wordStart != range.start) {
                selected = -1;
                first = 0;
                wordStart = range.start;
                values.clear();
                layout = null;
                CommandCompletionPresentation.clear(owner.get());
            }
            requests.beginRequest(prefix, nextClientValues);
        }

        private Request acceptResponse(String currentPrefix) {
            return requests.acceptResponse(currentPrefix);
        }

        private void move(int delta) {
            if (selected < 0) selected = delta < 0 ? values.size() - 1 : 0;
            else selected = (selected + delta + values.size()) % values.size();
            if (selected < first) first = selected;
            if (selected >= first + ChatSuggestionPopup.MAX_VISIBLE) {
                first = selected - ChatSuggestionPopup.MAX_VISIBLE + 1;
            }
            first = Math.max(0, Math.min(first,
                    Math.max(0, values.size() - ChatSuggestionPopup.MAX_VISIBLE)));
            CommandCompletionPresentation.update(owner.get(), values, selected);
        }

        private void commit(int index) {
            if (index < 0 || index >= values.size()) return;
            GuiTextField field = owner.get();
            if (field == null) return;
            String text = field.getText();
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
            TokenRange range = tokenRange(text, cursor);
            String insertion = insertionValue(text, range, values.get(index));
            field.setCursorPosition(range.start);
            field.setSelectionPos(range.end);
            field.writeText(insertion);
            dismiss();
        }

        private void dismiss() {
            values.clear();
            layout = null;
            requests.deactivate();
            selected = -1;
            first = 0;
            wordStart = -1;
            CommandCompletionPresentation.clear(owner.get());
        }
    }

    static final class RequestTracker {
        private String lastRequest = "";
        private long nextRequestId;
        private long activeRequestId = -1;
        private final Deque<Request> pendingRequests = new ArrayDeque<>();

        boolean shouldRequest(String prefix) {
            return !prefix.equals(lastRequest);
        }

        void beginRequest(String prefix, String[] clientValues) {
            lastRequest = prefix;
            Request request = new Request(++nextRequestId, prefix, clientValues);
            pendingRequests.addLast(request);
            activeRequestId = request.id;
        }

        Request acceptResponse(String currentPrefix) {
            Request request = pendingRequests.pollFirst();
            if (request == null || request.id != activeRequestId
                    || !request.prefix.equals(currentPrefix)) return null;
            return request;
        }

        void deactivate() {
            activeRequestId = -1;
        }
    }

    static final class Request {
        final long id;
        final String prefix;
        final String[] clientValues;

        Request(long id, String prefix, String[] clientValues) {
            this.id = id;
            this.prefix = prefix;
            this.clientValues = clientValues == null
                    ? new String[0] : clientValues.clone();
        }
    }

    static final class TokenRange {
        final int start;
        final int end;

        TokenRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
