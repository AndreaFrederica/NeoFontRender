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
import neofontrender.addons.build.UiBuildFeatures;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** UIE-owned command completion engine for the embedded Tabby chat input. */
public final class ChatCommandCompletionController {
    public static final ChatCommandCompletionController INSTANCE =
            new ChatCommandCompletionController();

    /**
     * Diagnostic switch, -Dnfr.debug.commandCompletion=true. Reports every Tab that advances or
     * fails to advance the candidate cycle, so a report like "Tab only ever inserts the first
     * candidate" can be told apart from "the cycle was dropped between keystrokes".
     */
    private static final boolean DEBUG =
            UiBuildFeatures.DIAGNOSTIC_LOGS && Boolean.getBoolean("nfr.debug.commandCompletion");

    /** Commands already known to hand Forge an unmodifiable completion list; warned about once. */
    private static final Set<String> UNSAFE_CLIENT_COMPLETION =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final String[] EMPTY_VALUES = new String[0];

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

    private State state(GuiTextField field, boolean create) {
        State state = create ? states.computeIfAbsent(field, State::new) : states.get(field);
        if (state != null) state.refreshPreferences();
        return state;
    }

    private boolean handle(GuiTextField field, int keyCode) {
        State state = state(field, false);
        if (!enabled(field) || !field.isFocused()) return false;
        if (state == null) return false;
        if (keyCode == Keyboard.KEY_TAB) {
            // Cycling needs the candidate list to outlive the popup. The cycle is owned by State, so
            // committing (which dismisses the popup) no longer strands Tab on an empty list, which is
            // what used to make every Tab after the first one a silent no-op.
            if (state.cycle != null) {
                if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
                    debug("tab selected=" + state.cycle.selected() + " n=" + state.cycle.values().size()
                            + " text=[" + field.getText() + "]");
                }
                state.commit(state.cycle.selected());
                // PREGEN drives its own completer index, so it must not advance on top of a commit.
                if (!CommandCompletionOptions.PREGEN.equals(state.engine)) state.moveCycle(1);
                if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) debug("tab advanced to=" + state.cycle.selected());
                return true;
            }
            // No cycle yet: ask for candidates. Logged because "Tab does nothing" can mean either
            // "no candidates came back" or "this branch never ran", and the two need different fixes.
            boolean slashPrefix = field.getCursorPosition() > 0 && field.getText().startsWith("/");
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
                debug("tab no-cycle cursor=" + field.getCursorPosition() + " text=["
                        + field.getText() + "] slash=" + slashPrefix);
            }
            // A Tab while awaiting candidates must not start vanilla's untracked second request.
            if (slashPrefix) {
                request(field, true);
                return true;
            }
            return false;
        }
        if (state.cycle == null || state.cycle.values().isEmpty()) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            state.dismiss();
            return true;
        }
        if (CommandCompletionOptions.HIDDEN.equals(state.display)) return false;
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (state.cycle.selected() >= 0) {
                state.commit(state.cycle.selected());
                request(field);
                return true;
            }
            return false;
        }
        if (keyCode == Keyboard.KEY_UP) {
            state.moveCycle(-1);
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            state.moveCycle(1);
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
        if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
            debug("afterKey key=" + keyCode + " text=[" + field.getText()
                    + "] cursor=" + field.getCursorPosition());
        }
        // Typing does not end the cycle here: beginRequest() drops it as soon as the edited text
        // starts a different token. Ending it on every keystroke would re-break cycling, because
        // the Tab that follows a completion arrives before the next server response does.
        request(field, false);
    }

    private void request(GuiTextField field) {
        request(field, false);
    }

    private void request(GuiTextField field, boolean force) {
        if (field == null || !field.isFocused()) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) debug("request skipped: field=" + (field == null ? "null" : "present")
                    + " focused=" + (field != null && field.isFocused()));
            return;
        }
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        String prefix = text.substring(0, cursor);
        if (prefix.isEmpty() || !prefix.startsWith("/")) {
            close(field);
            return;
        }
        State state = state(field, true);
        // Dedup by prefix alone would strand a prefix forever whenever the response for it never
        // produced a cycle (dropped, filtered to nothing, or dismissed). Tab used to become a
        // permanent no-op on "/" exactly that way: the prefix never changes, so shouldRequest()
        // stayed false and no request could ever be sent again. An explicit Tab may always retry.
        if (!state.shouldRequest(prefix, force)) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) debug("request deduped prefix=[" + prefix + "]");
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || minecraft.player.connection == null) return;
        BlockPos target = targetBlock(minecraft);
        if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
            debug("autoComplete BEGIN prefix=[" + prefix + "] space=" + prefix.indexOf(' '));
        }
        // Forge's ClientCommandHandler.autoComplete() rewrites the list it receives from
        // getTabCompletions() in place, so any command that hands back an unmodifiable view (or
        // otherwise fails while completing) aborts the whole frame. Vanilla only calls this on Tab;
        // we call it on every keystroke, so the failure has to be contained here and the client-side
        // candidates are simply skipped for that command.
        String[] clientValues = EMPTY_VALUES;
        try {
            ClientCommandHandler.instance.autoComplete(prefix);
            clientValues = ClientCommandCompletionApi.resolve(prefix,
                    completionPosition(target), ClientCommandHandler.instance.latestAutoComplete);
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
                debug("autoComplete OK prefix=[" + prefix + "] n="
                        + (clientValues == null ? -1 : clientValues.length));
            }
        } catch (RuntimeException | LinkageError failure) {
            if (UNSAFE_CLIENT_COMPLETION.add(rootOf(prefix))) {
                LOGGER.warn("Command /{} failed while completing; its client-side suggestions are"
                        + " skipped. Server-side suggestions still apply. Cause: {}",
                        rootOf(prefix), failure.getClass().getName(), failure);
            } else if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
                debug("autoComplete failed again prefix=[" + prefix + "] type="
                        + failure.getClass().getName());
            }
        }
        state.beginRequest(prefix, tokenRange(text, cursor), clientValues);
        if (CommandCompletionOptions.PREGEN.equals(state.engine) && state.bridge != null) {
            state.bridge.request(field);
        } else {
            minecraft.player.connection.sendPacket(new CPacketTabComplete(prefix, target, false));
        }
    }

    /** Root command name of a {@code /} prefixed input, used to remember unsupported completions. */
    private static String rootOf(String prefix) {
        String trimmed = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        int end = trimmed.indexOf(' ');
        return end < 0 ? trimmed : trimmed.substring(0, end);
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
        State state = state(field, false);
        if (state == null) return;
        String text = field.getText();
        int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
        String currentPrefix = text.substring(0, cursor);
        Request accepted = state.acceptResponse(currentPrefix);
        if (accepted == null || !enabled(field)) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
                debug("response dropped: accepted=" + (accepted != null));
            }
            return;
        }
        TokenRange range = tokenRange(text, cursor);
        String current = text.substring(range.start, range.end);
        boolean pregenEngine = CommandCompletionOptions.PREGEN.equals(state.engine) && state.bridge != null;
        if (!pregenEngine && keepsCommittedValue(state.committedRange(), range, current, values)) {
            // The committed word is still the whole token and the response does not contain it, which
            // is what a prefix-filtered server list looks like once an option is complete. Keep the
            // candidates this cycle started from instead of replacing them with a one-entry list.
            if (!valuesContain(values, current)) return;
            values = appendValue(values, current);
        }
        CommandCompletionCandidates.Merge merged = CommandCompletionCandidates.merge(values, accepted.clientValues);
        List<String> next = pregenEngine ? state.bridge.candidates(field, merged) : merged.styledValues();
        CommandCompletionPresentation.rememberRootCandidates(currentPrefix, next);
        boolean exactMatch = next.stream().anyMatch(value -> sameCandidate(value, current));
        if (!pregenEngine && !state.isCycling() && exactMatch && (cursor < range.end || next.size() == 1)) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
                debug("dismiss: exactMatch reason next=" + next.size()
                        + " current=[" + current + "] cursor=" + cursor
                        + " range=" + range.start + "-" + range.end);
            }
            state.dismiss();
            return;
        }
        if (!pregenEngine) next.removeIf(value -> sameCandidate(value, current));
        if (next.isEmpty()) {
            if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) debug("dismiss: no candidates after filtering current=[" + current + "]");
            state.dismiss();
            return;
        }
        if (UiBuildFeatures.DIAGNOSTIC_LOGS && DEBUG) {
            debug("cycle built n=" + next.size() + " range=" + range.start + "-" + range.end);
        }
        List<String> plain = plainOf(next);
        state.resetCycle(range.tokens(current), plain, next);
        state.openPopup();
    }

    /** Plain text of a styled candidate list, used when identity must ignore the color codes. */
    private static List<String> plainOf(List<String> values) {
        List<String> plain = new ArrayList<>(values.size());
        for (String value : values) plain.add(CommandCompletionCandidates.plain(value));
        return plain;
    }

    /**
     * Row offset of the visible window so that {@code selected} stays on screen.
     *
     * <p>Kept pure so the "highlight scrolled out of the window" regression stays covered: moving
     * the highlight without re-clamping this left the selected row outside the popup, and Tab then
     * silently inserted a candidate the user could not see.
     */
    static int viewportFirst(int first, int selected, int size) {
        int visible = ChatSuggestionPopup.MAX_VISIBLE;
        int clamped = first;
        if (selected < clamped) clamped = selected;
        if (selected >= clamped + visible) clamped = selected - visible + 1;
        return Math.max(0, Math.min(clamped, Math.max(0, size - visible)));
    }
    /** True while a response belongs to a word that Tab just completed and the cycle is still usable. */
    static boolean keepsCommittedValue(TokenRange committed, TokenRange current, String currentToken,
                                       String[] responseValues) {
        if (committed == null || current == null || currentToken == null) return false;
        if (committed.start != current.start || committed.end != current.end) return false;
        if (currentToken.isEmpty()) return false;
        return !valuesContain(responseValues, currentToken);
    }

    private static boolean valuesContain(String[] values, String token) {
        if (values == null) return false;
        for (String value : values) {
            if (sameCandidate(value, token)) return true;
        }
        return false;
    }

    private static String[] appendValue(String[] values, String value) {
        String[] appended = Arrays.copyOf(values == null ? new String[0] : values,
                (values == null ? 0 : values.length) + 1);
        appended[appended.length - 1] = value;
        return appended;
    }

    private static void debug(String message) {
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            LOGGER.info("[commandCompletion] {}", message);
        }
    }

    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("neofontrender.uie.completion");

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
        State state = state(field, false);
        if (!enabled(field) || state == null || state.cycle == null) return;
        List<String> values = state.cycle.values();
        if (values.isEmpty()) return;
        ChatSuggestionPopup.Layout layout = state.layout;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && layout != null && layout.rowAt(mouseX(), mouseY()) >= 0) {
            // The wheel moves the selection, exactly like the arrow keys, instead of scrolling the
            // viewport on its own. Tab then inserts the highlighted row the user is looking at.
            state.moveCycle(wheel > 0 ? -1 : 1);
            event.setCanceled(true);
            return;
        }
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        int row = layout == null ? -1 : layout.rowAt(mouseX(), mouseY());
        if (row < 0 || state.first + row >= values.size()) return;
        state.commit(state.first + row);
        state.moveCycle(1);
        request(field);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiChat)) return;
        GuiTextField field = field((GuiChat) event.getGui());
        State state = state(field, false);
        if (!enabled(field) || state == null || state.cycle == null) return;
        List<String> values = state.cycle.values();
        if (values.isEmpty()) return;
        if (CommandCompletionOptions.HIDDEN.equals(state.display)) {
            state.layout = null;
            return;
        }
        ExternalChatCompat.InputGeometry geometry = ExternalChatCompat.getSalutationInput(field);
        state.layout = CommandCompletionOptions.PREGEN.equals(state.display) && state.bridge != null
                ? state.bridge.draw(field, values, state.first, state.cycle.selected(), geometry,
                        event.getMouseX(), event.getMouseY(), Minecraft.getMinecraft().fontRenderer)
                : ChatSuggestionPopup.draw(field, values, state.first, state.cycle.selected(), geometry,
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
        State state = state(field, false);
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

    /**
     * One Tab cycle: the candidate list plus where in it the user is.
     *
     * <p>This owns the list instead of {@link State}, because committing a candidate dismisses the
     * popup. When the list lived on {@code State}, that dismissal destroyed the very data the next
     * Tab needed, so every Tab after the first became a silent no-op. A cycle is now only discarded
     * when the user leaves the token it belongs to, presses Escape/Enter, or the engine changes.
     *
     * <p>Identity ({@link #plain}) and display ({@link #display}) are tracked separately so cycling
     * can be compared against server responses without being confused by the source color codes.
     */
    static final class Cycle {
        private final TokenRange range;
        private final List<String> plain;
        private final List<String> display;
        private int selected = -1;

        Cycle(TokenRange range, List<String> plain, List<String> display) {
            this.range = range;
            this.plain = plain;
            this.display = display;
        }

        TokenRange range() { return range; }
        List<String> values() { return display; }
        List<String> plainValues() { return plain; }
        int selected() { return selected; }
        void onCommit() { if (selected < 0 && !display.isEmpty()) selected = 0; }

        /** Moves the highlight only; {@link State#moveCycle} owns keeping the viewport in step. */
        void move(int delta) {
            int size = display.size();
            if (size <= 0) return;
            selected = selected < 0
                    ? (delta < 0 ? size - 1 : 0)
                    : (selected + delta + size) % size;
        }
    }

    private static final class State {
        /** Live Tab cycle; survives popup dismissal and is owned across responses. */
        private Cycle cycle;
        private ChatSuggestionPopup.Layout layout;
        private String engine = CommandCompletionOptions.UIE;
        private String display = CommandCompletionOptions.UIE;
        private boolean active;
        private PregenCompletionBridge bridge;
        private int first;
        private final RequestTracker requests = new RequestTracker();
        private final WeakReference<GuiTextField> owner;

        private State(GuiTextField owner) {
            this.owner = new WeakReference<>(owner);
        }

        private void refreshPreferences() {
            String nextEngine = CommandCompletionOptions.engine();
            String nextDisplay = CommandCompletionOptions.display();
            boolean nextActive = enabled(owner.get());
            if (active == nextActive && engine.equals(nextEngine) && display.equals(nextDisplay)) return;
            // Keep the pending FIFO so old protocol responses cannot be mistaken for new requests.
            dismiss();
            requests.resetPrefix();
            engine = nextEngine;
            display = nextDisplay;
            active = nextActive;
            bridge = CommandCompletionOptions.PREGEN.equals(engine) || CommandCompletionOptions.PREGEN.equals(display)
                    ? PregenCompletionBridge.create() : null;
            if (bridge == null) {
                engine = CommandCompletionOptions.engine(engine, false);
                display = CommandCompletionOptions.display(display, false);
            }
        }

        /**
         * {@code force} is set by an explicit Tab. Without it a prefix whose response never built a
         * cycle stays marked as already-requested, so the prefix can never be retried and Tab turns
         * into a permanent no-op.
         */
        private boolean shouldRequest(String prefix, boolean force) {
            if (force && cycle == null) return true;
            return requests.shouldRequest(prefix);
        }

        private void beginRequest(String prefix, TokenRange range, String[] nextClientValues) {
            // A delimiter starts a distinct candidate set, so the old cycle no longer applies.
            if (cycle != null && cycle.range().start != range.start) dismiss();
            requests.beginRequest(prefix, nextClientValues);
        }

        private Request acceptResponse(String currentPrefix) {
            return requests.acceptResponse(currentPrefix);
        }

        /**
         * Installs a candidate list for {@code range}, or keeps the existing list when the response
         * resolves to exactly the same candidates (the common case while Tab cycling, where a
         * prefix-filtered server reply comes back narrower than the list the cycle started from).
         */
        private void resetCycle(TokenRange range, List<String> plain, List<String> display) {
            if (cycle != null && cycle.range().equals(range) && cycle.plainValues().equals(plain)) {
                openPopup();
                return;
            }
            cycle = new Cycle(range, plain, display);
            first = 0;
        }

        /** Shows the popup for the current cycle without disturbing the cycle itself. */
        private void openPopup() {
            if (cycle == null) return;
            clampFirst();
            CommandCompletionPresentation.update(owner.get(), cycle.values(), cycle.selected());
        }

        private boolean isCycling() {
            return cycle != null;
        }

        /**
         * Moves the highlight and drags the viewport along with it.
         *
         * <p>Both the arrow keys and the mouse wheel must go through here. Moving the highlight
         * without re-clamping {@link #first} let the selected row leave the visible window, so the
         * list appeared frozen while Tab silently inserted an off-screen candidate. Scrolling the
         * viewport without moving the highlight had the mirror problem: the highlight slid away and
         * Tab still inserted the old row.
         */
        private void moveCycle(int delta) {
            if (cycle == null) return;
            cycle.move(delta);
            clampFirst();
            CommandCompletionPresentation.update(owner.get(), cycle.values(), cycle.selected());
        }

        private TokenRange committedRange() {
            return cycle == null ? null : cycle.range();
        }

        /** Ends the cycle: the popup closes and the candidate list is discarded. */
        private void dismiss() {
            cycle = null;
            layout = null;
            requests.deactivate();
            first = 0;
            CommandCompletionPresentation.clear(owner.get());
        }

        private void clampFirst() {
            if (cycle == null) return;
            first = viewportFirst(first, cycle.selected(), cycle.values().size());
        }

        private void commit(int index) {
            if (cycle == null) return;
            List<String> values = cycle.values();
            if (index < 0 || index >= values.size()) return;
            GuiTextField field = owner.get();
            if (field == null) return;
            if (CommandCompletionOptions.PREGEN.equals(engine) && bridge != null
                    && bridge.commit(field, values, index)) {
                cycle.onCommit();
                layout = null;
                CommandCompletionPresentation.clear(field);
                return;
            }
            String text = field.getText();
            int cursor = Math.max(0, Math.min(field.getCursorPosition(), text.length()));
            TokenRange range = tokenRange(text, cursor);
            String insertion = insertionValue(text, range, values.get(index));
            field.setCursorPosition(range.start);
            field.setSelectionPos(range.end);
            field.writeText(insertion);
            // The committed token keeps its range, so the response for it cannot collapse the list.
            cycle.onCommit();
            layout = null;
            CommandCompletionPresentation.clear(field);
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

        void resetPrefix() {
            lastRequest = "";
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

        /** The span this token occupies once {@code inserted} replaces it at the cursor. */
        TokenRange tokens(String inserted) {
            return new TokenRange(start, start + (inserted == null ? 0 : inserted.length()));
        }

        /** Value equality, so a cycle can be matched against the token a response was computed for. */
        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof TokenRange)) return false;
            TokenRange range = (TokenRange) other;
            return start == range.start && end == range.end;
        }

        @Override
        public int hashCode() {
            return start * 31 + end;
        }

        @Override
        public String toString() {
            return start + "-" + end;
        }
    }
}
