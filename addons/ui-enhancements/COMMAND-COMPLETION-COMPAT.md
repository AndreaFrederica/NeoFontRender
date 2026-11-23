# Integrated chat completion

The enhanced chat settings save two independent preferences:

- `chat.commandCompletionEngine`: `uie` (default) or `pregenerator`.
- `chat.commandCompletionDisplay`: `uie` (default), `pregenerator`, or `hidden`.

`chat.commandCompletion` remains the master switch. Hidden display retains Tab
completion but does not intercept Up/Down/Enter or show inline ghost suggestions.
The selectors affect UIE's integrated chat. Disabled integrated chat leaves the
external mod's original chat behavior available. Restart after changing ownership
of the entire chat/HUD; the two completion selectors do not require a restart.

The Pregenerator adapter invokes the installed `ChatScreen$Completor` on a mirror
text field. Requests, candidate filtering and insertion use its implementation
when its engine is selected. Rendering imports the unified candidate/selection
snapshot into the completer without enabling a second input handler or packet
sender. The display option never changes the selected engine. Missing or
incompatible adapters fall back to UIE and log failures, without overwriting the
saved preference. Unavailable providers are omitted from the dropdowns.

Only the `disableAdvChat` reads in Pregenerator's `ClientHandler.onGuiOpen` are
overridden while UIE owns chat. Both HUD and input replacement are suppressed;
other options and warning dialogs retain their original values. No Pregenerator
configuration file is modified. The compatibility mixin is queued after mod
discovery and has no hard dependency on Pregenerator or CarbonConfig classes.

Verified against Chunk-Pregenerator-1.12.2-4.4.9.3.jar. Optional binary contract
test: pass `-PpregenCompatJar=<path>` when running `PregenBinaryContractTest`.
Regression tests cover provider combinations, fallback, option isolation, and
discarding old responses after switching with identical input. In-game checks:
all four engine/display combinations, hidden display, `/pregen` argument
completion, fast typing/Tab before responses, mouse scroll/click, chat scale,
sleep chat, disabling integrated chat and starting without Pregenerator.
