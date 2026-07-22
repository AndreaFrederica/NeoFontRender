# Neo Font Render integration API

NFR API version 3 exposes TOML storage, client settings-page registration, and information-page
contributions. Integrations should
declare NFR as a required or optional compile-time dependency as appropriate.

## TOML configuration

Dedicated files are the default and recommended mode:

```java
NfrConfigFile config = NfrConfigApi.builder("examplemod")
        .fileName("examplemod.toml")
        .open();
config.define("feature.enabled", true, "Enable the feature.");
boolean enabled = config.getBoolean("feature.enabled", true);
config.set("feature.enabled", false).save();
```

An integration may explicitly store values in the NFR main file instead:

```java
NfrConfigFile config = NfrConfigApi.builder("examplemod")
        .storage(NfrConfigStorage.NFR_MAIN)
        .open();
```

Those keys are isolated below `extensions.examplemod` in `neofontrender.toml`. The Modern Tooltips
addon intentionally uses `config/neofontrender-modern-tooltips.toml`, not the main file.

## Embedded settings pages

Register an `NfrSettingsPage` during client initialization:

```java
NfrSettingsPageRegistry.register(new ExampleSettingsPage());
```

Each opening of the NFR settings screen calls `createSession()`. The returned session builds its
view with NFR's shared controls and receives `apply()` or `cancel()` from the common footer. Keep
editable values in the session (or snapshot runtime values) so Cancel can restore them. Page ids
must be namespaced, such as `examplemod:settings`.

Addon pages are placed after NFR's functional pages. The built-in About and Open Source Licenses
pages always remain at the bottom of the sidebar.

## About and license contributions

Addons can append attribution, project links, or bundled dependency licenses without creating a
separate settings tab:

```java
NfrInfoPageRegistry.register(new NfrInfoPageContribution() {
    public String id() { return "examplemod:licenses"; }
    public NfrInfoPage page() { return NfrInfoPage.LICENSES; }
    public List<NfrInfoLine> lines() {
        return Arrays.asList(
                NfrInfoLine.spaced("Example Mod - MIT", 0xD8D8D8),
                NfrInfoLine.line("Bundled Library - Apache-2.0", 0xD8D8D8));
    }
});
```

Register during client pre-initialization. Contribution ids must be namespaced; `order()` controls
their order within the selected information page. Supplier-based lines may be used for dynamic or
localized text.

## Arc3D

NFR distributes Arc3D Core as an unrelocated contained library. Integrations can use the original
`icyllis.arc3d.*` packages or the stable, explicitly named `neofontrender.api.arc3d.Arc3DApi`
entry point. Do not bundle a second Arc3D copy when NFR is a required dependency.
