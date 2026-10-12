# Command completion provider API

Revo UI separates command completion by authority. Server providers run inside the integrated or
dedicated server and may use permissions or dynamic server state. Client providers are local-only
and must not claim that a remote server accepts a command.

## Shared result modes

- `PASS`: this provider does not handle the request.
- `FALLBACK`: use these candidates only when earlier/original candidates are empty.
- `APPEND`: merge these candidates with the current list.
- `REPLACE`: intentionally replace the current list.

Providers run by descending priority and then stable registration ID. Candidate order is retained,
duplicates are removed, and one provider failure is isolated from the remaining providers.

## Server provider

Register through `ServerCommandCompletionApi`. The context exposes the actual `ICommand`, sender,
server, typed root alias, arguments, target position, and current candidates. This is the correct
side for online-player names, permission-sensitive values, and server registries.

```java
ServerCommandCompletionApi.registerProvider(
        new ResourceLocation("example", "legacy_command"),
        0,
        context -> context.commandClassIs("example.LegacyCommand")
                ? CommandCompletionResult.fallback(
                        context.matchingLastArgument(Arrays.asList("one", "two")))
                : CommandCompletionResult.pass());
```

TC6 is the first built-in server provider. It is activated only when Forge reports Thaumcraft as
loaded and the resolved command is exactly TC6's `CommandThaumcraft` implementation.

## Client provider

Register through `ClientCommandCompletionApi`. Forge client-command candidates enter the pipeline
as the original list. Local providers can supplement client commands without being reported as
server candidates.

```java
ClientCommandCompletionApi.registerProvider(
        new ResourceLocation("example", "client_command"),
        0,
        context -> context.rootIs("local")
                ? CommandCompletionResult.append(
                        context.matchingLastArgument(Arrays.asList("show", "hide")))
                : CommandCompletionResult.pass());
```

The version-specific boundaries are limited to the vanilla `CommandHandler` Mixin and chat input
bridges. Optional-mod adapters depend on the provider contracts instead of adding their own global
command-manager Mixins.
